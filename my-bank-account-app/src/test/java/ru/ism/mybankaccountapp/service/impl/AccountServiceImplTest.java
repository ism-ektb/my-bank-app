package ru.ism.mybankaccountapp.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankaccountapp.service.NotificationService;
import ru.ism.mybankdto.module.AccountRequestDto;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.CashMoney;
import ru.ism.mybankdto.module.Transfer;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"spring.liquibase.default-schema=account_service", "spring.liquibase.enabled=true"
})
@Testcontainers
class AccountServiceImplTest {

    @Autowired
    private AccountService accountService;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;
    @MockitoBean
    private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;
    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository;


    @Container
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withInitScript("schema1.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.liquibase.user", postgreSQLContainer::getUsername);
        registry.add("spring.liquibase.password", postgreSQLContainer::getPassword);
        registry.add("spring.r2dbc.url", () -> postgreSQLContainer.getJdbcUrl().replace("jdbc:", "r2dbc:"));
        registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername);
        registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword);
    }

    @Test
    void find_account_if_no_account() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser0")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNull(dto);
    }

    @Test
    @WithMockUser(authorities = { "account.write" })
    void create_account() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser6")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        AccountResponseDto dto = accountService.createAccount(token).block();
        assertNotNull(dto);
        assertEquals("testUser6", dto.login());
    }

    @Test
    @WithMockUser(authorities = { "account.read" })
    void create_account_no_authorities() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(AuthorizationDeniedException.class, () -> accountService.createAccount(token).block());
    }

    @Test
    @WithMockUser(authorities = { "account.write" })
    void update_account() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.createAccount(token).block();
        accountService.updateAccount(new AccountRequestDto("testName", null), token).block();
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        assertEquals("testName", dto.name());

    }

    @Test
    /**
     * Создаем счет. Пополняем его. Снимаем средства одновременно несколькими потоками
     */
    @WithMockUser(authorities = { "account.write" })
    void cash_integral_test() throws InterruptedException {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser2")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.createAccount(token).block();
        accountService.addSum(new CashMoney("testUser2", 100L)).block();
        int count = 10;
        CountDownLatch latch = new CountDownLatch(count);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    accountService.reduceSum(new CashMoney("testUser2", 1L)).block();
                } finally {
                    latch.countDown();
                }
            }
        };
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(runnable);
            thread.start();

        }
        latch.await();
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        assertEquals(90L, dto.balance());

    }

    /**
     * Создаем два новых счета. Один пополняем.
     * В многопоточном режиме переводим средства с одного счета на другой.
     * Проверяем что бы сумма средств на двух счетах не изменилась.
     * В ходе теста могут выбрасываться исключения - это его нормальная работа.
     */
    @Test
    @WithMockUser(authorities = { "account.write" })
    void transfer_integral_test() throws InterruptedException {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken5")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser5")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.createAccount(token).block();
        accountService.addSum(new CashMoney("testUser5", 8L)).block();
        Jwt jwt1 = Jwt.withTokenValue("testToken1")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser1")
                .build();
        JwtAuthenticationToken token1 = new JwtAuthenticationToken(jwt1);
        accountService.createAccount(token1).block();
        int count = 10;
        CountDownLatch latch = new CountDownLatch(count);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    accountService.transfer(new Transfer("testUser5", "testUser1", 8L)).block();
                    accountService.transfer(new Transfer("testUser1", "testUser5", 8L)).block();
                } finally {
                    latch.countDown();
                }
            }
        };
        for (
                int i = 0;
                i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
        latch.await();
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        AccountResponseDto dto1 = accountService.findAccount(token1).block();
        assertNotNull(dto1);
        assertEquals(8L, dto.balance() + dto1.balance());
    }
}