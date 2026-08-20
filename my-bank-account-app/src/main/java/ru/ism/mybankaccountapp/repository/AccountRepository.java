package ru.ism.mybankaccountapp.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.model.Account;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Mono<Account> findByLogin(String accountNumber);

    Mono<Account> findByName(String name);

    @Query("WITH rows AS (UPDATE account_service.accounts SET balance = balance - :sum WHERE login = :login AND balance >= :sum returning *) SELECT count(*) from rows")
    Mono<Integer> reduceBalance(@Param("login") String login, @Param("sum") Long sum);

    @Query("with rows AS (UPDATE account_service.accounts SET balance = balance + :sum WHERE login = :login RETURNING *) SELECT count(*) from rows")
    Mono<Integer> addBalance(@Param("login") String login, @Param("sum") Long sum);


}
