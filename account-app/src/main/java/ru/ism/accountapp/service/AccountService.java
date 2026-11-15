package ru.ism.accountapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.accountapp.domain.Account;
import ru.ism.accountapp.domain.Currency;
import ru.ism.accountapp.dto.ApiDtos.AccountCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.AccountResponse;
import ru.ism.accountapp.repository.AccountRepository;
import ru.ism.accountapp.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Mono<AccountResponse> createForUser(String userLogin, AccountCreateRequest request) {
        return userRepository.findByLogin(userLogin)
                .flatMap(user -> accountRepository.findByUserLogin(userLogin)
                        .filter(account -> account.currency().equals(request.currency().toString()))
                        .hasElements()
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Account with this currency already exists for user"
                                ));
                            } else {
                                String accountNumber = "ACC-" + userLogin + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                                Account account = new Account(
                                        null,
                                        user.login(),
                                        accountNumber,
                                        request.initialBalance(),
                                        request.currency().toString(),
                                        LocalDateTime.now(),
                                        LocalDateTime.now()
                                );

                                return accountRepository.save(account)
                                        .map(AccountResponse::from);
                            }
                        })

                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))));
    }

    public Mono<Void> deleteForUser(String userLogin, Currency currency) {
        return userRepository.findByLogin(userLogin)
                .flatMap(user -> accountRepository.findByUserLogin(userLogin)
                        .filter(account -> account.currency().equals(currency.toString()))
                        .singleOrEmpty()
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                        .filter(account -> account.balance().compareTo(BigDecimal.ZERO) == 0)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account balance must be zero to delete")))
                        .flatMap(account -> accountRepository.delete(account))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))));
    }

    public Flux<AccountResponse> findByUserLogin(String userLogin) {
        return accountRepository.findByUserLogin(userLogin)
                .map(AccountResponse::from);
    }

    public Mono<AccountResponse> getById(Long accountId) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                .map(AccountResponse::from);
    }

    public Mono<AccountResponse> changeBalance(Long accountId, BigDecimal delta) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                .flatMap(account -> {
                    BigDecimal newBalance = account.balance().add(delta);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Not enough funds on account"
                        ));
                    }

                    Account updated = new Account(
                            account.id(),
                            account.userLogin(),
                            account.number(),
                            newBalance,
                            account.currency(),
                            account.createdAt(),
                            LocalDateTime.now()
                    );

                    return accountRepository.save(updated)
                            .map(AccountResponse::from);
                });
    }
}
