package ru.ism.accountapp.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/users/{userLogin}/accounts")
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            @PathVariable String userLogin,
            @Valid @RequestBody AccountCreateRequest request
    ) {
        return accountService.createForUser(userLogin, request)
                .map(account -> ResponseEntity.status(HttpStatus.CREATED).body(account));
    }

    @DeleteMapping("/users/{userLogin}/accounts/{currency}")
    public Mono<ResponseEntity<Void>> deleteAccount(
            @PathVariable String userLogin,
            @PathVariable Currency currency
    ) {
        return accountService.deleteForUser(userLogin, currency)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/{userLogin}/accounts")
    public Flux<AccountResponse> getUserAccounts(@PathVariable String userLogin) {
        return accountService.findByUserLogin(userLogin);
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
