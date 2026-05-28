package ru.ism.mybankaccountapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.model.AccountRequestDto;
import ru.ism.mybankaccountapp.model.AccountResponseDto;
import ru.ism.mybankaccountapp.model.ChangeAccountRequestDto;
import ru.ism.mybankaccountapp.service.AccountService;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping
    public AccountResponseDto updateAccount(@RequestBody AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        return accountService.updateAccount(accountRequestDto, authentication);
    }

    @GetMapping
    public AccountResponseDto getAccount(JwtAuthenticationToken authentication) {
        System.out.println(authentication);
        return accountService.findAccount(authentication);
    }


}
