package ru.ism.mybanktransferapp.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;
import ru.ism.mybanktransferapp.service.NotificationTransferService;

import java.util.concurrent.CompletableFuture;
@Service
@Slf4j
public class NotificationTransferServiceImpl implements NotificationTransferService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationTransferServiceImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<Void> sendNotification(Notification notification) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("transfer", "transfer_service", notification.notification());
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(exception.getMessage(), exception);
            }
        });
        return Mono.empty();
    }
}
