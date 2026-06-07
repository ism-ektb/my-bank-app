package ru.ism.mybankaccountapp;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.model.Account;

import java.util.Optional;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Mono<Account> findByLogin(String accountNumber);

    Mono<Account> findByName(String name);
}
