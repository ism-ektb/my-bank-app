package ru.ism.notificationapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ism.notificationapp.client.UserClient;



@RestController
@RequestMapping("/api")
public class NotificationController {

    private final UserClient userClient;
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    public NotificationController(UserClient userClient) {
        this.userClient = userClient;
    }

    @PostMapping("/notification")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public void logNotification(@RequestParam(name = "text") String text){
        var user = userClient.getUser();
        log.info("Пользователь {} {}", user, text);

    }

}
