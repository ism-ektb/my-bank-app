package ru.ism.accountapp.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.accountapp.domain.Account;

import java.math.BigDecimal;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Flux<Account> findByUserLogin(String userLogin);

    Mono<Account> findFirstAccountByUserLoginAndCurrency(String login, String currency);

    @Query("WITH rows AS (UPDATE user_accounts SET balance = balance - :sum, updated_at = NOW() WHERE user_login = :login AND currency = :currency AND balance >= :sum returning *) SELECT count(*) from rows")
    Mono<Integer> reduceBalance(@Param("login") String login, @Param("sum") BigDecimal sum, @Param("currency") String currency);

    @Query("with rows AS (UPDATE user_accounts SET balance = balance + :sum, updated_at = NOW() WHERE user_login = :login AND currency = :currency RETURNING *) SELECT count(*) from rows")
    Mono<Integer> addBalance(@Param("login") String login, @Param("sum") BigDecimal sum, @Param("currency") String currency);
}