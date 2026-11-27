package ru.ism.accountapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                        .flatMap(accountRepository::delete));
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

    @Transactional
    public Mono<Void> addBalance(String login, Currency currency, BigDecimal sum) {
        if (sum == null || sum.signum() <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive"));
        }

        return accountRepository.findFirstAccountByUserLoginAndCurrency(login, currency.toString())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                .then(accountRepository.addBalance(login, sum, currency.toString())
                        .flatMap(updatedRows -> {
                            if (updatedRows == 1) {
                                return Mono.empty();
                            }
                            if (updatedRows == 0) {
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
                            }
                            return Mono.error(new IllegalStateException("More than one account matched the deposit"));
                        }))
                .then();
    }

    @Transactional
    public Mono<Void> reduceBalance(String login, Currency currency, BigDecimal sum) {
        if (sum == null || sum.signum() <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive"));
        }

        return accountRepository.findFirstAccountByUserLoginAndCurrency(login, currency.toString())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                .then(accountRepository.reduceBalance(login, sum, currency.toString())
                        .flatMap(updatedRows -> {
                            if (updatedRows == 1) {
                                return Mono.empty();
                            }
                            if (updatedRows == 0) {
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
                            }
                            return Mono.error(new IllegalStateException("More than one account matched the deposit"));
                        }))
                .then();
    }

    @Transactional
    public Mono<Void> transferBalance(
            String fromLogin,
            String toLogin,
            Currency currency,
            BigDecimal sum
    ) {
        if (sum == null || sum.signum() <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive"));
        }
        if (fromLogin.equals(toLogin)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must differ"));
        }

        return Mono.when(
                        accountRepository.findFirstAccountByUserLoginAndCurrency(fromLogin, currency.toString())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found")))
                                .then(),
                        accountRepository.findFirstAccountByUserLoginAndCurrency(toLogin, currency.toString())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found")))
                                .then()
                )
                .then(accountRepository.reduceBalance(fromLogin, sum, currency.toString()))
                .flatMap(withdrawnRows -> {
                    if (withdrawnRows == 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough funds on source account"));
                    }
                    if (withdrawnRows != 1) {
                        return Mono.error(new IllegalStateException("More than one source account matched the transfer"));
                    }

                    return accountRepository.addBalance(toLogin, sum, currency.toString())
                            .flatMap(creditedRows -> {
                                if (creditedRows == 1) {
                                    return Mono.empty();
                                }
                                if (creditedRows == 0) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found"));
                                }
                                return Mono.error(new IllegalStateException("More than one destination account matched the transfer"));
                            });
                })
                .then();
    }

    @Transactional
    public Mono<Void> convertBalance(
            Currency toCurrency,
            BigDecimal toSum,
            BigDecimal fromSum,
            Currency fromCurrency,
            String login
    ) {
        if (toSum == null || toSum.signum() <= 0 || fromSum == null || fromSum.signum() <= 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amounts must be positive"));
        }
        if (toCurrency == fromCurrency) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currencies must differ"));
        }

        return Mono.when(
                        accountRepository.findFirstAccountByUserLoginAndCurrency(login, fromCurrency.toString())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found")))
                                .then(),
                        accountRepository.findFirstAccountByUserLoginAndCurrency(login, toCurrency.toString())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found")))
                                .then()
                )
                .then(accountRepository.reduceBalance(login, fromSum, fromCurrency.toString()))
                .flatMap(withdrawnRows -> {
                    if (withdrawnRows == 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough funds on source account"));
                    }
                    if (withdrawnRows != 1) {
                        return Mono.error(new IllegalStateException("More than one source account matched the conversion"));
                    }

                    return accountRepository.addBalance(login, toSum, toCurrency.toString())
                            .flatMap(creditedRows -> {
                                if (creditedRows == 1) {
                                    return Mono.empty();
                                }
                                if (creditedRows == 0) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found"));
                                }
                                return Mono.error(new IllegalStateException("More than one destination account matched the conversion"));
                            });
                })
                .then();
    }
}
