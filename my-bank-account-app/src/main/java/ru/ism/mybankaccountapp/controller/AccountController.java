package ru.ism.mybankaccountapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankdto.module.*;

/**
 * Контроллер операций с банковскими счетами.
 *
 * <p>Предоставляет REST-эндпоинты для создания учетного счета, обновления
 * данных счета, пополнения и списания средств, перевода между счетами и
 * получения информации о счетах текущего пользователя или другого клиента.</p>
 *
 * @author MyBank Team
 * @version 1.0
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;
    Logger log = LoggerFactory.getLogger(AccountController.class);

    /**
     * Обновляет данные существующего банковского счёта.
     *
     * @param accountRequestDto данные счёта для обновления
     * @param authentication токен аутентификации текущего пользователя
     * @return реактивный объект с обновлённой информацией о счёте
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'SERVICE') && hasAuthority('account.write')")
    public Mono<AccountResponseDto> updateAccount(@RequestBody AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        log.info("update account");
        return accountService.updateAccount(accountRequestDto, authentication);
    }

    /**
     * Создаёт счёт пользователя, если он ещё не существует.
     *
     * @param authentication токен аутентификации пользователя
     * @return реактивный объект с данными созданного или существующего счёта
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Mono<AccountResponseDto> createAccountIfNotExist(JwtAuthenticationToken authentication) {
        log.info("create account ");
        return accountService.findAccount(authentication)
                .switchIfEmpty(accountService.createAccount(authentication));
    }

    /**
     * Списывает средства со счёта клиента.
     *
     * @param cashMoney данные о сумме и логине клиента
     * @param authentication токен аутентификации сервиса
     * @return обновлённая информация о счёте после списания
     */
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/debit")
    public Mono<AccountResponseDto> debit(@RequestBody @Valid CashMoney cashMoney, JwtAuthenticationToken authentication) {
        log.info("debit account login: {}", cashMoney.login());
        return accountService.addSum(cashMoney);
    }

    /**
     * Зачисляет средства на счёт клиента.
     *
     * @param cashMany данные о сумме и логине клиента
     * @return обновлённая информация о счёте после зачисления
     */
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/credit")
    public Mono<AccountResponseDto> credit(@RequestBody @Valid CashMoney cashMany) {
        log.info("credit account login: {}", cashMany.login());
        return accountService.reduceSum(cashMany);
    }

    /**
     * Выполняет перевод средств между счетами.
     *
     * @param transfer данные перевода: отправитель, получатель и сумма
     * @return пустой реактивный результат после успешного выполнения операции
     */
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PostMapping("/transfer")
    public Mono<Void> transfer(@RequestBody @Valid Transfer transfer) {
        log.info("transfer request, receiver = {}, sender = {}", transfer.receiver(), transfer.sender());
        return accountService.transfer(transfer);
    }

    /**
     * Возвращает информацию о счёте по логину клиента.
     *
     * @param login логин клиента
     * @return реактивный объект с данными счёта
     */
    @PreAuthorize("hasRole('SERVICE')")
    @GetMapping("/{login}")
    public Mono<AccountResponseDto> getAccountByName(@PathVariable String login) {
        log.info("get account by login: {}", login);
        return accountService.findByName(login);
    }

    /**
     * Возвращает список краткой информации по всем счетам текущего пользователя.
     *
     * @param authentication токен аутентификации пользователя
     * @return поток краткой информации о счетах
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/all")
    public Flux<AccountShortResponse> getAllAccounts(JwtAuthenticationToken authentication) {
        log.info("get all account list");
        return accountService.findAllWithoutUser(authentication);
    }


}
