package ru.ism.accountapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.ism.accountapp.domain.Currency;
import ru.ism.accountapp.dto.ApiDtos.AccountCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.AccountResponse;
import ru.ism.accountapp.dto.ApiDtos.BalanceChangeRequest;
import ru.ism.accountapp.dto.ApiDtos.ConvertRequest;
import ru.ism.accountapp.dto.ApiDtos.TransferRequest;
import ru.ism.accountapp.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    private AccountService accountService;
    private AccountController controller;
    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken("alice", "token");
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final AccountResponse account = new AccountResponse(
            10L, "alice", "ACC-alice-USD", new BigDecimal("25.00"), "USD", timestamp, timestamp);

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        controller = new AccountController(accountService);
    }

    @Test
    void createAccountReturnsCreatedResponseForAuthenticatedUser() {
        AccountCreateRequest request = new AccountCreateRequest(Currency.USD, new BigDecimal("25.00"));
        when(accountService.createForUser("alice", request)).thenReturn(Mono.just(account));

        StepVerifier.create(controller.createAccount(request, authentication))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals(account, response.getBody());
                })
                .verifyComplete();

        verify(accountService).createForUser("alice", request);
    }

    @Test
    void deleteAccountDelegatesForAuthenticatedUserAndCurrency() {
        when(accountService.deleteForUser("alice", Currency.USD)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteAccount(Currency.USD, authentication))
                .verifyComplete();

        verify(accountService).deleteForUser("alice", Currency.USD);
    }

    @Test
    void getUserAccountsReturnsAccountsForAuthenticatedUser() {
        when(accountService.findByUserLogin("alice")).thenReturn(Flux.just(account));

        StepVerifier.create(controller.getUserAccounts(authentication))
                .expectNext(account)
                .verifyComplete();

        verify(accountService).findByUserLogin("alice");
    }

    @Test
    void getAccountReturnsOkResponse() {
        when(accountService.getById(10L)).thenReturn(Mono.just(account));

        StepVerifier.create(controller.getAccount(10L))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(account, response.getBody());
                })
                .verifyComplete();

        verify(accountService).getById(10L);
    }

    @Test
    void addBalanceDelegatesRequestValuesAndAuthenticatedLogin() {
        BalanceChangeRequest request = new BalanceChangeRequest(new BigDecimal("5.00"), Currency.USD);
        when(accountService.addBalance("alice", Currency.USD, new BigDecimal("5.00")))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.addBalance(request, authentication))
                .verifyComplete();

        verify(accountService).addBalance("alice", Currency.USD, new BigDecimal("5.00"));
    }

    @Test
    void reduceBalanceDelegatesRequestValuesAndAuthenticatedLogin() {
        BalanceChangeRequest request = new BalanceChangeRequest(new BigDecimal("5.00"), Currency.USD);
        when(accountService.reduceBalance("alice", Currency.USD, new BigDecimal("5.00")))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.reduceBalance(request, authentication))
                .verifyComplete();

        verify(accountService).reduceBalance("alice", Currency.USD, new BigDecimal("5.00"));
    }

    @Test
    void convertBalanceDelegatesRequestValuesAndAuthenticatedLogin() {
        ConvertRequest request = new ConvertRequest(
                Currency.EUR, Currency.USD, new BigDecimal("9.00"), new BigDecimal("10.00"));
        when(accountService.convertBalance(
                Currency.EUR, new BigDecimal("9.00"), new BigDecimal("10.00"), Currency.USD, "alice"))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.convertBalance(request, authentication))
                .verifyComplete();

        verify(accountService).convertBalance(
                Currency.EUR, new BigDecimal("9.00"), new BigDecimal("10.00"), Currency.USD, "alice");
    }

    @Test
    void transferDelegatesRequestValuesAndAuthenticatedLogin() {
        TransferRequest request = new TransferRequest("bob", Currency.USD, new BigDecimal("5.00"));
        when(accountService.transferBalance("alice", "bob", Currency.USD, new BigDecimal("5.00")))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.transfer(request, authentication))
                .verifyComplete();

        verify(accountService).transferBalance("alice", "bob", Currency.USD, new BigDecimal("5.00"));
    }
}
