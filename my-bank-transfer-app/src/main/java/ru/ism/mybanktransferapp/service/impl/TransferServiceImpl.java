package ru.ism.mybanktransferapp.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.Notification;
import ru.ism.mybankdto.module.Transfer;
import ru.ism.mybankdto.module.TransferRequest;
import ru.ism.mybanktransferapp.service.TransferService;

@Service
public class TransferServiceImpl implements TransferService {

    @Autowired
    private WebClient webClient;
    @Value("${bank.accounts-service.base-url}")
    private String baseUrl;
    @Value("${bank.notification-url}")
    private String notificationUrl;

    @Override
    public Mono<Void> transfer(TransferRequest transferRequest, JwtAuthenticationToken jwtAuthenticationToken) {
        String senderLogin = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");

        return checkMoney(transferRequest.sum(), senderLogin)
                .then(transferMoney(transferRequest.name(), senderLogin, transferRequest.sum()));
    }

    private Mono<Void> checkMoney(long amount, String senderLogin) {
        return webClient.get()
                .uri(baseUrl + "/account/" + senderLogin)
                .retrieve()
                .bodyToMono(AccountResponseDto.class)
                .filter(dto -> dto.balance() >= amount)
                .switchIfEmpty(Mono.error(new RuntimeException("Sender have not enough money")))
                .then();
    }

    private Mono<Void> transferMoney(String receiverLogin, String senderLogin, long amount) {
        return webClient.post()
                .uri(baseUrl + "/account/transfer")
                .bodyValue(new Transfer(senderLogin, receiverLogin, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .then(webClient.post()
                        .uri(notificationUrl + "/notification")
                        .bodyValue(new Notification(String.format("Переведены средства со счета %s на счет %s в сумме %d", senderLogin, receiverLogin, amount)))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(throwable -> Mono.empty()));
    }
}
