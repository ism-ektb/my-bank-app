package ru.ism.mybankaccountapp.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.service.NotificationService;
import ru.ism.mybankdto.module.Notification;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

        public NotificationServiceImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<Void> sendNotification(Notification notification) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("account", "account_service", notification.notification());
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(exception.getMessage(), exception);
            }
        });
        return Mono.empty();
    }
}
