package ru.ism.transferapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.ism.transferapp.client.NotificationClient;
import ru.ism.transferapp.client.TransferClient;
import ru.ism.transferapp.config.TransferSecurityConfig;
import ru.ism.transferapp.domain.Currency;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(TransferController.class)
@Import(TransferSecurityConfig.class)
class TransferControllerSecurityTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private TransferClient transferClient;

    @MockitoBean
    private NotificationClient notificationClient;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void transferRequiresAuthentication() {
        webTestClient.mutateWith(csrf())
                .put().uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(transferClient, notificationClient);
    }

    @Test
    void transferRequiresWriteAuthority() {
        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .put().uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(transferClient, notificationClient);
    }

    @Test
    void transferRequiresUserRole() {
        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(transferClient, notificationClient);
    }

    @Test
    void transferAllowsUserWithWriteAuthority() {
        when(transferClient.transferBalance(any(TransferClient.TransferRequest.class)))
                .thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлен перевод сумму 50.00 USD ползователю с логином bob"))
                .thenReturn(Mono.empty());

        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isOk();

        verify(transferClient).transferBalance(new TransferClient.TransferRequest(
                "bob", Currency.USD, new BigDecimal("50.00")));
        verify(notificationClient).send("Осуществлен перевод сумму 50.00 USD ползователю с логином bob");
    }

    @Test
    void transferSucceedsWhenNotificationFails() {
        when(transferClient.transferBalance(any(TransferClient.TransferRequest.class)))
                .thenReturn(Mono.empty());
        when(notificationClient.send(any()))
                .thenReturn(Mono.error(new IllegalStateException("notification unavailable")));

        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isOk();
    }

    private String requestBody() {
        return "{\"toLogin\":\"bob\",\"currency\":\"USD\",\"sum\":50.00}";
    }
}
