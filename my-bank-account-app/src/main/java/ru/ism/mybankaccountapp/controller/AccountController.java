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

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;
    Logger log = LoggerFactory.getLogger(AccountController.class);

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'SERVICE') && hasAuthority('account.write')")
    public Mono<AccountResponseDto> updateAccount(@RequestBody AccountRequestDto accountRequestDto, JwtAuthenticationToken authentication) {
        log.info("update account");
        return accountService.updateAccount(accountRequestDto, authentication);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Mono<AccountResponseDto> createAccountIfNotExist(JwtAuthenticationToken authentication) {
        log.info("create account ");
        return accountService.findAccount(authentication)
                .switchIfEmpty(accountService.createAccount(authentication));
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/debit")
    public Mono<AccountResponseDto> debit(@RequestBody @Valid CashMoney cashMoney, JwtAuthenticationToken authentication) {
        log.info("debit account login: {}", cashMoney.login());
        return accountService.addSum(cashMoney);
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PutMapping("/credit")
    public Mono<AccountResponseDto> credit(@RequestBody @Valid CashMoney cashMany) {
        log.info("credit account login: {}", cashMany.login());
        return accountService.reduceSum(cashMany);
    }

    @PreAuthorize("hasRole('SERVICE') && hasAuthority('account.write')")
    @PostMapping("/transfer")
    public Mono<Void> transfer(@RequestBody @Valid Transfer transfer) {
        log.info("transfer request, receiver = {}, sender = {}", transfer.receiver(), transfer.sender());
        return accountService.transfer(transfer);
    }

    @PreAuthorize("hasRole('SERVICE')")
    @GetMapping("/{login}")
    public Mono<AccountResponseDto> getAccountByName(@PathVariable String login) {
        log.info("get account by login: {}", login);
        return accountService.findByName(login);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/all")
    public Flux<AccountShortResponse> getAllAccounts(JwtAuthenticationToken authentication) {
        log.info("get all account list");
        return accountService.findAllWithoutUser(authentication);
    }


}
