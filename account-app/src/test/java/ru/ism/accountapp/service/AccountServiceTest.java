package ru.ism.accountapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.ism.accountapp.domain.Account;
import ru.ism.accountapp.domain.Currency;
import ru.ism.accountapp.domain.User;
import ru.ism.accountapp.dto.ApiDtos.AccountCreateRequest;
import ru.ism.accountapp.repository.AccountRepository;
import ru.ism.accountapp.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        accountService = new AccountService(accountRepository, userRepository);
    }

    @Test
    void createForUserSavesAccountWhenCurrencyIsNotAlreadyUsed() {
        var request = new AccountCreateRequest(Currency.USD, new BigDecimal("25.00"));
        when(userRepository.findByLogin("alice")).thenReturn(Mono.just(user("alice")));
        when(accountRepository.findByUserLogin("alice")).thenReturn(Flux.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            return Mono.just(new Account(1L, saved.userLogin(), saved.number(), saved.balance(), saved.currency(),
                    saved.createdAt(), saved.updatedAt()));
        });

        StepVerifier.create(accountService.createForUser("alice", request))
                .assertNext(response -> {
                    assertEquals(1L, response.id());
                    assertEquals("alice", response.userLogin());
                    assertEquals("USD", response.currency());
                    assertEquals(new BigDecimal("25.00"), response.balance());
                })
                .verifyComplete();

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createForUserRejectsDuplicateCurrency() {
        var request = new AccountCreateRequest(Currency.USD, BigDecimal.TEN);
        when(userRepository.findByLogin("alice")).thenReturn(Mono.just(user("alice")));
        when(accountRepository.findByUserLogin("alice"))
                .thenReturn(Flux.just(account("alice", "USD", BigDecimal.ZERO)));

        StepVerifier.create(accountService.createForUser("alice", request))
                .expectErrorSatisfies(error -> {
                    var responseException = (ResponseStatusException) error;
                    assertEquals(HttpStatus.CONFLICT, responseException.getStatusCode());
                })
                .verify();

        verify(accountRepository, never()).save(any(Account.class));
    }



    @Test
    void addBalanceRejectsZeroOrNegativeAmount() {
        StepVerifier.create(accountService.addBalance("alice", Currency.USD, BigDecimal.ZERO))
                .expectErrorSatisfies(error -> {
                    var responseException = (ResponseStatusException) error;
                    assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatusCode());
                })
                .verify();

        verifyNoInteractions(accountRepository);
    }

    @Test
    void addBalanceCompletesWhenRepositoryUpdatesOneAccount() {
        when(accountRepository.findFirstAccountByUserLoginAndCurrency("alice", "USD"))
                .thenReturn(Mono.just(account("alice", "USD", BigDecimal.TEN)));
        when(accountRepository.addBalance("alice", new BigDecimal("5.00"), "USD"))
                .thenReturn(Mono.just(1));

        StepVerifier.create(accountService.addBalance("alice", Currency.USD, new BigDecimal("5.00")))
                .verifyComplete();

        verify(accountRepository).addBalance("alice", new BigDecimal("5.00"), "USD");
    }

    @Test
    void transferBalanceRejectsTransferToSameUser() {
        StepVerifier.create(accountService.transferBalance("alice", "alice", Currency.USD, BigDecimal.ONE))
                .expectErrorSatisfies(error -> {
                    var responseException = (ResponseStatusException) error;
                    assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatusCode());
                })
                .verify();

        verifyNoInteractions(accountRepository);
    }

    @Test
    void transferBalanceRejectsInsufficientFunds() {
        when(accountRepository.findFirstAccountByUserLoginAndCurrency("alice", "USD"))
                .thenReturn(Mono.just(account("alice", "USD", BigDecimal.TEN)));
        when(accountRepository.findFirstAccountByUserLoginAndCurrency("bob", "USD"))
                .thenReturn(Mono.just(account("bob", "USD", BigDecimal.ZERO)));
        when(accountRepository.reduceBalance("alice", new BigDecimal("15.00"), "USD"))
                .thenReturn(Mono.just(0));

        StepVerifier.create(accountService.transferBalance(
                        "alice", "bob", Currency.USD, new BigDecimal("15.00")))
                .expectErrorSatisfies(error -> {
                    var responseException = (ResponseStatusException) error;
                    assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatusCode());
                })
                .verify();

        verify(accountRepository, never()).addBalance(anyString(), any(), anyString());
    }

    private static User user(String login) {
        var now = LocalDateTime.now();
        return new User(1L, "Alice", login, LocalDate.of(1990, 1, 1), "password", now, now);
    }

    private static Account account(String login, String currency, BigDecimal balance) {
        var now = LocalDateTime.now();
        return new Account(1L, login, "ACC-" + login, balance, currency, now, now);
    }
}
