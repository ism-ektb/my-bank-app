package ru.ism.cashapp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ism.cashapp.client.CashClient;
import ru.ism.cashapp.client.NotificationClient;

@RestController
@RequestMapping("/api")
public class CashController {

    private final CashClient cashClient;
    private final NotificationClient notificationClient;

    public CashController(CashClient cashClient, NotificationClient notificationClient) {
        this.cashClient = cashClient;
        this.notificationClient = notificationClient;
    }

    @PutMapping("/add")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public Mono<Void> addBalance(@RequestBody CashClient.BalanceChangeRequest request) {
        return cashClient.addBalance(request.currency(), request.delta())
                .then(notificationClient.send(String.format("Осуществлено пополнение счета на сумму %s %s", request.delta(), request.currency()))
                        .onErrorResume(e -> Mono.empty()));
    }

    @PutMapping("/reduce")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public Mono<Void> addReduce(@RequestBody CashClient.BalanceChangeRequest request) {
        return cashClient.reduceBalance(request.currency(), request.delta())
                .then(notificationClient.send(String.format("Осуществлено снятие со счета на сумму %s %s", request.delta(), request.currency()))
                        .onErrorResume(e -> Mono.empty()));
    }


}
