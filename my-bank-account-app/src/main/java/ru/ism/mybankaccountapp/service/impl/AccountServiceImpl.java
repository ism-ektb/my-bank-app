package ru.ism.mybankaccountapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.repository.AccountRepository;
import ru.ism.mybankaccountapp.mapper.AccountMapper;
import ru.ism.mybankaccountapp.model.Account;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankaccountapp.service.NotificationService;
import ru.ism.mybankdto.exception.NoFoundException;
import ru.ism.mybankdto.exception.ValidationException;
import ru.ism.mybankdto.module.*;

import java.util.Objects;


@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final NotificationService notificationService;

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

    /**
     * Добавляем деньги на счете пользователя и отправляем уведомление об этом
     *
     * @param cashMany
     * @return
     */
    @Override
    @Transactional
    public Mono<AccountResponseDto> addSum(CashMoney cashMany) {
        return accountRepository.findByLogin(cashMany.login())
                .switchIfEmpty(Mono.error(new NoFoundException("Пользователь не найден")))
                .then(accountRepository.addBalance(cashMany.login(), cashMany.sum())
                        .filter(a -> a == 1)
                        .switchIfEmpty(Mono.error(new ValidationException("Ошибка пополнения средств"))))
                .then(accountRepository.findByLogin(cashMany.login()))
                .map(accountMapper::toAccountResponseDto)
                .flatMap(dto -> notificationService
                        .sendNotification(new Notification(String.format("Счет %s пополнен на сумму %d", cashMany.login(), cashMany.sum())))
                        .onErrorResume(e -> Mono.empty())
                        .then(Mono.just(dto)));
    }

    /**
     * Снимаем деньги со счета пользователя и отправляем уведомление
     *
     * @param cashMany
     * @return
     */
    @Override
    @Transactional
    public Mono<AccountResponseDto> reduceSum(CashMoney cashMany) {
        return accountRepository.findByLogin(cashMany.login())
                .switchIfEmpty(Mono.error(new NoFoundException("Пользователь не найден")))
                .then(accountRepository.reduceBalance(cashMany.login(), cashMany.sum())
                        .filter(a -> a == 1)
                        .switchIfEmpty(Mono.error(new ValidationException("Ошибка списания средств"))))
                .then(accountRepository.findByLogin(cashMany.login()))
                .map(accountMapper::toAccountResponseDto)
                .flatMap(dto -> notificationService
                        .sendNotification(new Notification(String.format("Счет %s уменьшен на сумму %d", cashMany.login(), cashMany.sum())))
                        .onErrorResume(e -> Mono.empty())
                        .then(Mono.just(dto)));


    }

    /**
     * Перевод средств с одного счета на другой в соответствии с запросом
     *
     * @param transfer
     * @return
     */
    @Override
    @Transactional
    public Mono<Void> transfer(Transfer transfer) {
        return accountRepository.findByLogin(transfer.sender())
                .switchIfEmpty(Mono.error(new NoFoundException("Отправитель не найден")))
                .then(accountRepository.findByLogin(transfer.receiver())
                        .switchIfEmpty(Mono.error(new NoFoundException("Получатель не найден"))))
                .then(accountRepository.reduceBalance(transfer.sender(), transfer.sum())
                        .filter(a -> a == 1)
                        .switchIfEmpty(Mono.error(new ValidationException("Ошибка списания средств")))
                        .then(accountRepository.addBalance(transfer.receiver(), transfer.sum())
                                .filter(a -> a == 1)
                                .switchIfEmpty(Mono.error(new ValidationException("Ошибка пополнения счета")))))
                .then(notificationService
                        .sendNotification(new Notification(String.format("Успешный перевод со счета %s на счет %s на сумму %d",
                                transfer.sender(), transfer.receiver(), transfer.sum())))
                        .onErrorResume(e -> Mono.empty()));
    }

    @Override
    public Mono<AccountResponseDto> findByName(String accountName) {
        return accountRepository.findByLogin(accountName)
                .map(accountMapper::toAccountResponseDto);
    }

    /**
     * Поиск всех пользователей за исключением инициатора поиска
     *
     * @param jwtAuthenticationToken
     * @return
     */
    @Override
    public Flux<AccountShortResponse> findAllWithoutUser(JwtAuthenticationToken jwtAuthenticationToken) {
        String login = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");
        return accountRepository.findAll()
                .filter(account -> !Objects.equals(account.getLogin(), login))
                .map(accountMapper::toAccountShortResponse);
    }
}
