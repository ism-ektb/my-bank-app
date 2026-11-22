package ru.ism.accountapp.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.accountapp.domain.Currency;
import ru.ism.accountapp.dto.ApiDtos.AccountCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.AccountResponse;
import ru.ism.accountapp.dto.ApiDtos.BalanceChangeRequest;
import ru.ism.accountapp.service.AccountService;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    @PostMapping("/users/accounts")
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request,
            Authentication authentication
    ) {
        return accountService.createForUser(authentication.getName(), request)
                .map(account -> ResponseEntity.status(HttpStatus.CREATED).body(account));
    }

    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    @DeleteMapping("/users/accounts/{currency}")
    public Mono<ResponseEntity<Void>> deleteAccount(
            @PathVariable Currency currency,
            Authentication authentication
    ) {
        return accountService.deleteForUser(authentication.getName(), currency)
                .map(ResponseEntity::ok);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/users/accounts")
    public Flux<AccountResponse> getUserAccounts(Authentication authentication) {
        return accountService.findByUserLogin(authentication.getName());
    }

    @GetMapping("/accounts/{id}")
    public Mono<ResponseEntity<AccountResponse>> getAccount(@PathVariable Long id) {
        return accountService.getById(id)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/accounts/{id}/balance")
    public Mono<ResponseEntity<AccountResponse>> changeBalance(
            @PathVariable Long id,
            @Valid @RequestBody BalanceChangeRequest request
    ) {
        return accountService.changeBalance(id, request.delta())
                .map(ResponseEntity::ok);
    }
}
