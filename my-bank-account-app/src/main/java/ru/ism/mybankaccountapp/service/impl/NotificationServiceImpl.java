package ru.ism.mybankaccountapp.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.service.NotificationService;
import ru.ism.mybankdto.module.Notification;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private WebClient webClient;
    @Value("${bank.notification}")
    private String bankNotificationUrl;

    @Override
    public Mono<Void> sendNotification(Notification notification) {
        return webClient.post()
                .uri(bankNotificationUrl + "/notification")
                .bodyValue(notification)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> Mono.empty());
    }
}
