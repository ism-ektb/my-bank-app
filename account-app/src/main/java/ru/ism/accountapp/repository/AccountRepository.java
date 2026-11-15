package ru.ism.accountapp.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.ism.accountapp.domain.Account;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Flux<Account> findByUserLogin(String userLogin);
}