package ru.ism.mybanktransferapp.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.exception.ClientException;
import ru.ism.mybankdto.exception.ValidationException;
import ru.ism.mybankdto.module.*;
import ru.ism.mybanktransferapp.service.NotificationTransferService;
import ru.ism.mybanktransferapp.service.TransferService;

@Service
public class TransferServiceImpl implements TransferService {

    private final WebClient webClient;
    private final String accountUrl;
    private final NotificationTransferService notificationService;
    private final MeterRegistry registry;
    private final Counter counter;

    public TransferServiceImpl(WebClient webClient,
                               @Value("${bank.accounts-service.base-url}") String accountUrl,
                               NotificationTransferService notificationService, MeterRegistry registry) {
        this.webClient = webClient;
        this.accountUrl = accountUrl;
        this.notificationService = notificationService;
        this.registry = registry;
        this.counter = Counter.builder("error_transfer_service").register(registry);
    }

    @Override
    public Mono<Void> transfer(TransferRequest transferRequest, JwtAuthenticationToken jwtAuthenticationToken) {
        String senderLogin = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");
        return checkMoney(transferRequest.sum(), senderLogin)
                .then(transferMoney(transferRequest.name(), senderLogin, transferRequest.sum()));
    }

    private Mono<Void> checkMoney(long amount, String senderLogin) {
        String str = senderLogin;
        return webClient.get()
                .uri(accountUrl + "/account/" + senderLogin)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, responseEntity -> {
                    counter.increment();
                    return Mono.error(new ClientException("TransferService client Error. Status code: " + responseEntity.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, responseEntity -> {
                    counter.increment();
                    return Mono.error(new ClientException("AccountService server Error. Status code: " + responseEntity.statusCode()));
                })
                .bodyToMono(AccountResponseDto.class)
                .filter(dto -> dto.balance() >= amount)
                .switchIfEmpty(Mono.error(new ValidationException("Sender have not enough money")))
                .doOnError(throwable -> {
                    counter.increment();
                })
                .then();

    }

    private Mono<Void> transferMoney(String receiverLogin, String senderLogin, long amount) {
        return webClient.post()
                .uri(accountUrl + "/account/transfer")
                .bodyValue(new Transfer(senderLogin, receiverLogin, amount))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, responseEntity -> {
                    registry.counter("error_transfer_service", "login", senderLogin).increment();
                    return Mono.error(new ClientException("TransferService client Error. Status code: " + responseEntity.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, responseEntity -> {
                    registry.counter("error_transfer_service", "login", senderLogin).increment();
                    return Mono.error(new ClientException("AccountService server Error. Status code: " + responseEntity.statusCode()));
                })
                .bodyToMono(Void.class)
                .then(notificationService.sendNotification(new Notification(
                                String.format("Переведены средства со счета %s на счет %s в сумме %d", senderLogin, receiverLogin, amount)))
                        .onErrorResume(throwable -> Mono.empty()));
    }
}
