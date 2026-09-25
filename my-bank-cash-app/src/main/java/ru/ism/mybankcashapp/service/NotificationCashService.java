package ru.ism.mybankcashapp.service;

import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

/**
 * Определяет отправку уведомлений cash-сервисом.
 */
public interface NotificationCashService {

    Mono<Void> sendNotification(Notification notification);
}
