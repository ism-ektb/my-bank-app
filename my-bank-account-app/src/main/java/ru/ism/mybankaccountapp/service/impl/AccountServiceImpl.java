package ru.ism.mybankaccountapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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


/**
 * Реализация сервиса для управления банковскими счетами.
 *
 * <p>Сервис отвечает за обновление данных счетов, поиск учетных записей,
 * создание новых счетов, пополнение и списание средств, а также переводы между
 * счетами и отправку уведомлений.</p>
 *
 * @author MyBank Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final NotificationService notificationService;

    /**
     * Обновляет данные счёта текущего пользователя.
     *
     * @param accountRequestDto новые данные счёта
     * @param authentication токен аутентификации текущего пользователя
     * @return реактивный объект с обновлённым профилем счёта
     */
    @Override
    public Mono<AccountResponseDto> updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new NoFoundException(String.format("Login %s not found", login))))
                .map(account -> {
                    accountMapper.updateAccount(account, accountRequestDto);
                    return account;
                })
                .flatMap(accountRepository::save)
                .map(accountMapper::toAccountResponseDto);
    }

    /**
     * Ищет счёт текущего пользователя по токену аутентификации.
     *
     * @param authentication токен аутентификации пользователя
     * @return реактивный объект со счётом, если он существует
     */
    @Override
    public Mono<AccountResponseDto> findAccount(JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        return accountRepository.findByLogin(login)
                .map(accountMapper::toAccountResponseDto);
    }

    /**
     * Пополняет счёт пользователя и отправляет уведомление.
     *
     * @param cashMany данные о сумме и логине клиента
     * @return реактивный объект с обновлённым балансом счёта
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
     * Снимает деньги со счёта пользователя и отправляет уведомление.
     *
     * @param cashMany данные о сумме и логине клиента
     * @return реактивный объект с обновлённым балансом счёта
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
     * Выполняет перевод средств с одного счёта на другой.
     *
     * @param transfer параметры перевода: отправитель, получатель и сумма
     * @return пустой реактивный результат при успешном завершении перевода
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

    /**
     * Возвращает данные счёта по логину клиента.
     *
     * @param accountName логин клиента
     * @return реактивный объект с данными счёта
     */
    @Override
    public Mono<AccountResponseDto> findByName(String accountName) {
        return accountRepository.findByLogin(accountName)
                .map(accountMapper::toAccountResponseDto);
    }

    /**
     * Возвращает список счётов всех пользователей, кроме текущего.
     *
     * @param jwtAuthenticationToken токен аутентификации текущего пользователя
     * @return поток краткой информации о счётах других пользователей
     */
    @Override
    public Flux<AccountShortResponse> findAllWithoutUser(JwtAuthenticationToken jwtAuthenticationToken) {
        String login = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");
        return accountRepository.findAll()
                .filter(account -> !Objects.equals(account.getLogin(), login))
                .map(accountMapper::toAccountShortResponse);
    }

    /**
     * Создаёт новый счёт для пользователя при отсутствии существующего.
     *
     * @param authentication токен аутентификации пользователя
     * @return реактивный объект с данными созданного счёта
     */
    @Override
    @PreAuthorize("hasAuthority('account.write')")
    public Mono<AccountResponseDto> createAccount(JwtAuthenticationToken authentication) {
        String login = authentication.getToken().getClaimAsString("preferred_username");
        Account newAccount = new Account();
        newAccount.setLogin(login);
        return accountRepository.save(newAccount)
                .map(accountMapper::toAccountResponseDto);
    }
}
