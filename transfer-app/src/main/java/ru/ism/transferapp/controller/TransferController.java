package ru.ism.transferapp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.ism.transferapp.client.NotificationClient;
import ru.ism.transferapp.client.TransferClient;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final TransferClient transferClient;
    private final NotificationClient notificationClient;

    public TransferController(TransferClient transferClient, NotificationClient notificationClient) {
        this.transferClient = transferClient;
        this.notificationClient = notificationClient;
    }

    @PutMapping("/transfer")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public Mono<Void> transfer(@RequestBody TransferClient.TransferRequest request){
        return transferClient.transferBalance(request)
                .then(notificationClient.send(String.format("Осуществлен перевод сумму %s %s ползователю с логином %s", request.sum(), request.currency(), request.toLogin()))
                        .onErrorResume(e -> Mono.empty()));
    }
}
