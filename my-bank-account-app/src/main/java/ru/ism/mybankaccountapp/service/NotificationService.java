package ru.ism.mybankaccountapp.service;

import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

public interface NotificationService {

    Mono<Void> sendNotification(Notification notification);
}
