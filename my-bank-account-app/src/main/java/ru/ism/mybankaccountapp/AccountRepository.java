package ru.ism.mybankaccountapp;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.model.Account;

import java.util.Optional;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Mono<Account> findByLogin(String accountNumber);

    Mono<Account> findByName(String name);

    @Query("UPDATE account_service.accounts SET balance = balance - :sum WHERE login = :login")
    Mono<Void> reduceBalance(@Param("login") String login, @Param("sum") Long sum);

    @Query("UPDATE account_service.accounts SET balance = balance + :sum WHERE login = :login")
    Mono<Void> addBalance(@Param("login") String login, @Param("sum") Long sum);


}
