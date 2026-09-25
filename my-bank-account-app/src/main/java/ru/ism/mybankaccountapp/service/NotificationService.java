package ru.ism.mybankaccountapp.service;

import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

/**
 * Определяет отправку уведомлений аккаунт-сервисом.
 */
public interface NotificationService {

    Mono<Void> sendNotification(Notification notification);
}
