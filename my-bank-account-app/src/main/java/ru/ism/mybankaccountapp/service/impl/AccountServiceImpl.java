package ru.ism.mybankaccountapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.AccountRepository;
import ru.ism.mybankaccountapp.mapper.AccountMapper;
import ru.ism.mybankaccountapp.model.Account;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankdto.module.*;

import java.util.Objects;


@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    @Autowired
    private WebClient webClient;

    @Override
    public Mono<AccountResponseDto> updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        return accountRepository.findByLogin(login)
                .defaultIfEmpty(new Account())
                .map(account -> {
                    accountMapper.updateAccount(account, accountRequestDto);
                    return account;
                })
                .flatMap(accountRepository::save)
                .map(accountMapper::toAccountResponseDto);
    }

    @Override
    public Mono<AccountResponseDto> findAccount(JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        Account newAccount = new Account();
        newAccount.setLogin(login);

        return accountRepository.findByLogin(login)
                .switchIfEmpty(accountRepository.save(newAccount))
                .map(accountMapper::toAccountResponseDto);
    }

    @Override
    @Transactional
    public Mono<AccountResponseDto> addSum(CashMany cashMany) {
        System.out.println("cashMany: " + cashMany);
        return accountRepository.findByLogin(cashMany.login())
                .map(account -> {
                    long sum = account.getBalance();
                    sum += cashMany.sum();
                    account.setBalance(sum);
                    System.out.println(account);
                    return account;
                })
                .flatMap(accountRepository::save)
                .map(accountMapper::toAccountResponseDto)
                .flatMap(dto -> webClient.post()
                        .uri("http://localhost:8086/notification")
                        .bodyValue(new Notification(String.format("Счет %s пополнен на сумму %d", cashMany.login(), cashMany.sum())))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(e -> Mono.empty())
                        .then(Mono.just(dto)));

    }

    @Override
    @Transactional
    public Mono<AccountResponseDto> reduceSum(CashMany cashMany) {
        return accountRepository.findByLogin(cashMany.login())
                .map(account -> {
                    long sum = account.getBalance();
                    sum -= cashMany.sum();
                    account.setBalance(sum);
                    return account;
                })
                .filter(account -> account.getBalance() >= 0)
                .switchIfEmpty(Mono.error(new RuntimeException("No cash")))
                .flatMap(accountRepository::save)
                .map(accountMapper::toAccountResponseDto)
                .flatMap(dto -> webClient.post()
                        .uri("http://localhost:8086/notification")
                        .bodyValue(new Notification(String.format("Счет %s уменьшен на сумму %d", cashMany.login(), cashMany.sum())))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(e -> Mono.empty())
                        .then(Mono.just(dto)));
    }

    @Override
    @Transactional
    public Mono<Void> transfer(Transfer transfer) {
        return accountRepository.findByLogin(transfer.sender())
                .flatMap(account -> {
                    long sum = account.getBalance() - transfer.sum();
                    account.setBalance(sum);
                    return accountRepository.save(account);
                })
                .then(accountRepository.findByLogin(transfer.receiver())
                        .flatMap(account -> {
                            long sum = account.getBalance() + transfer.sum();
                            account.setBalance(sum);
                            return accountRepository.save(account);
                        })).then(webClient.post()
                        .uri("http://localhost:8086/notification")
                        .bodyValue(new Notification(String.format("Успешный перевод со счета %s на счет %s на сумму %d",
                                transfer.sender(), transfer.receiver(), transfer.sum())))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(e -> Mono.empty()));
    }

    @Override
    public Mono<AccountResponseDto> findByName(String accountName) {
        return accountRepository.findByLogin(accountName)
                .map(accountMapper::toAccountResponseDto);
    }

    @Override
    public Flux<AccountShortResponse> findAllWithoutUser(JwtAuthenticationToken jwtAuthenticationToken) {
        String login = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");
        return accountRepository.findAll()
                .filter(account -> !Objects.equals(account.getLogin(), login))
                .map(accountMapper::toAccountShortResponse);
    }
}
