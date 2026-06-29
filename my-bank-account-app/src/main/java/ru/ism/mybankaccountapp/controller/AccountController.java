package ru.ism.mybankaccountapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankdto.module.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'SERVICE') && hasAuthority('account.write')")
    public Mono<AccountResponseDto> updateAccount(@RequestBody AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        return accountService.updateAccount(accountRequestDto, authentication);
    }

    @GetMapping
    public Mono<AccountResponseDto> getAccount(JwtAuthenticationToken authentication) {
        return accountService.findAccount(authentication);
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/debit")
    public Mono<AccountResponseDto> debit(@RequestBody CashMany cashMany, JwtAuthenticationToken authentication) {
        return accountService.addSum(cashMany);
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/credit")
    public Mono<AccountResponseDto> credit(@RequestBody CashMany cashMany) {
        return accountService.reduceSum(cashMany);
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PostMapping("/transfer")
    public Mono<Void> transfer(@RequestBody Transfer transfer) {
        return accountService.transfer(transfer);
    }

    @GetMapping("/{login}")
    public Mono<AccountResponseDto> getAccountByName(@PathVariable String login) {
        return accountService.findByName(login);
    }

    @GetMapping("/all")
    public Flux<AccountShortResponse> getAllAccounts(JwtAuthenticationToken authentication) {
        return accountService.findAllWithoutUser(authentication);
    }


}
