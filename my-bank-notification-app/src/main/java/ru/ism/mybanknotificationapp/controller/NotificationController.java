package ru.ism.mybanknotificationapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Notification;

import static reactor.netty.http.HttpConnectionLiveness.log;

@RestController
@RequestMapping("/notification")

public class NotificationController {


    @PostMapping
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('notification.write')")
    public Mono<Void> logNotification(@RequestBody Notification notification, JwtAuthenticationToken authentication) {
        String serviceName = authentication.getToken().getClaimAsString("preferred_username");
        log.info("Сервис {} сообщение {}", serviceName, notification.notification());
        return Mono.empty();

    }
}
