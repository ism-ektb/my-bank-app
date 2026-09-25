package ru.ism.mybanktransferapp.service;

import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

/**
 * Определяет отправку уведомлений transfer-сервисом.
 */
public interface NotificationTransferService {

    Mono<Void> sendNotification(Notification notification);
}
