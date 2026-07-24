package ru.ism.mybankaccountapp.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.sql.init.mode=always")
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
    @ServiceConnection(type = {R2dbcConnectionDetails.class})
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15");

    @Test
    void find_account_if_no_account() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        assertEquals("testUser", dto.login());
    }

    @Test
    void update_account() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.findAccount(token).block();
        accountService.updateAccount(new AccountRequestDto("testName", null), token).block();
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        assertEquals("testName", dto.name());

    }

    @Test
    /**
     * Создаем счет. Пополняем его. Снимаем средства одновременно несколькими потоками
     */
    void cash_integral_test() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser2")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.findAccount(token).block();
        accountService.addSum(new CashMoney("testUser2", 100L)).block();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountService.reduceSum(new CashMoney("testUser2", 1L)).block();
            }
        };
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
        try {
            Thread.sleep(1000); // Пауза на 1 секунду. Ждем завершения всех потоков
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
    void transfer_integral_test() {
        when(notificationService.sendNotification(any())).thenReturn(Mono.empty());
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        accountService.findAccount(token).block();
        accountService.addSum(new CashMoney("testUser", 8L)).block();
        Jwt jwt1 = Jwt.withTokenValue("testToken1")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser1")
                .build();
        JwtAuthenticationToken token1 = new JwtAuthenticationToken(jwt1);
        accountService.findAccount(token1).block();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountService.transfer(new Transfer("testUser", "testUser1", 8L)).block();
                accountService.transfer(new Transfer("testUser1", "testUser", 8L)).block();
            }
        };
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
        try {
            Thread.sleep(1000); // Пауза на 1 секунду. Ждем завершения работы всех потоков
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AccountResponseDto dto = accountService.findAccount(token).block();
        assertNotNull(dto);
        AccountResponseDto dto1 = accountService.findAccount(token1).block();
        assertNotNull(dto1);
        assertEquals(8L, dto.balance() + dto1.balance());

    }

}