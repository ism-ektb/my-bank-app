package ru.ism.convertapp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.ism.convertapp.client.NotificationClient;
import ru.ism.convertapp.domain.Currency;
import ru.ism.convertapp.service.ConvertService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class ConvertController {

    private final ConvertService service;
    private final NotificationClient notificationClient;

    public ConvertController(ConvertService service, NotificationClient notificationClient) {
        this.service = service;
        this.notificationClient = notificationClient;
    }

    @PutMapping("/convert")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public Mono<Void> convertCurrency(@RequestBody FrontConvertRequest request ){
        return service.convertCurrency(request.toCurrency, request.fromCurrency, request.sum)
                .then(notificationClient.send(String.format("Осуществлена конвертация %s %s в %s" , request.sum, request.fromCurrency, request.toCurrency))
                        .onErrorResume(e -> Mono.empty()));
    }

    public record FrontConvertRequest(
             Currency toCurrency,
             Currency fromCurrency,
             BigDecimal sum){}
}
