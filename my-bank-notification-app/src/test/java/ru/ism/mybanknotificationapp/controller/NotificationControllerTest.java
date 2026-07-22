package ru.ism.mybanknotificationapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.ism.mybankdto.module.Notification;
import ru.ism.mybanknotificationapp.config.SecurityConfig;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void logNotification_good_result() {
        webClient.mutateWith(mockJwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                        new SimpleGrantedAuthority("notification.write")))
                .post()
                .uri("/notification")
                .bodyValue(new Notification("str"))
                .exchange().expectStatus().isOk();
    }

    @Test
    void logNotification_bad_authorities() {
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("notification.write")))
                .post()
                .uri("/notification")
                .bodyValue(new Notification("str"))
                .exchange().expectStatus().isEqualTo(403);
    }
}