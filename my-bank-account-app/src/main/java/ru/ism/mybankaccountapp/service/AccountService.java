package ru.ism.mybankaccountapp.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.*;

public interface AccountService {

    Mono<AccountResponseDto> updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication);

    Mono<AccountResponseDto> findAccount(JwtAuthenticationToken authentication);

    /**
     * Добавляем деньги на счет пользователя и отправляем уведомление
     * @param cashMany
     * @return
     */
    Mono<AccountResponseDto> addSum(CashMany cashMany);

    /**
     * Снимаем деньги со счета пользователя и отправляем уведомление
     * @param cashMany
     * @return
     */
    Mono<AccountResponseDto> reduceSum(CashMany cashMany);

    /**
     * Осуществляем перевод с одного счета на другой
     * @param transfer
     * @return
     */
    Mono<Void> transfer(Transfer transfer);

    Mono<AccountResponseDto> findByName(String accountName);

    /**
     * Поиск всех пользователей за исключением инициатора поиска
     * @param authentication
     * @return
     */
    Flux<AccountShortResponse> findAllWithoutUser(JwtAuthenticationToken authentication);


}
