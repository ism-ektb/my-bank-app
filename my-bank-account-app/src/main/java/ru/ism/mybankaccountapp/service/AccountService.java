package ru.ism.mybankaccountapp.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.*;

public interface AccountService {

    Mono<AccountResponseDto> updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication);

    Mono<AccountResponseDto> findAccount(JwtAuthenticationToken authentication);

    Mono<AccountResponseDto> addSum(CashMany cashMany);

    Mono<AccountResponseDto> reduceSum(CashMany cashMany);

    Mono<Void> transfer(Transfer transfer);

    Mono<AccountResponseDto> findByName(String accountName);

    Flux<AccountShortResponse> findAllWithoutUser(JwtAuthenticationToken authentication);


}
