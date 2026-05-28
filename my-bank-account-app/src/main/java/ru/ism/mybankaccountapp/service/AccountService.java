package ru.ism.mybankaccountapp.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.model.AccountRequestDto;
import ru.ism.mybankaccountapp.model.AccountResponseDto;
import ru.ism.mybankaccountapp.model.ChangeAccountRequestDto;


public interface AccountService {

    AccountResponseDto updateAccount(AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication);
    AccountResponseDto findAccount(JwtAuthenticationToken authentication);

}
