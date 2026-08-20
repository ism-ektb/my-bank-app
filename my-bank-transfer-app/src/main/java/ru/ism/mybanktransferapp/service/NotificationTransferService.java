package ru.ism.mybanktransferapp.service;

import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

public interface NotificationTransferService {

    Mono<Void> sendNotification(Notification notification);
}
