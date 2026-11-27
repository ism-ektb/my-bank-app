package ru.ism.convertapp.controller;

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
import ru.ism.convertapp.client.NotificationClient;
import ru.ism.convertapp.config.CashSecurityConfig;
import ru.ism.convertapp.domain.Currency;
import ru.ism.convertapp.service.ConvertService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(ConvertController.class)
@Import(CashSecurityConfig.class)
class ConvertControllerSecurityTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ConvertService convertService;

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
    void convertRequiresAuthentication() {
        webTestClient.mutateWith(csrf())
                .put().uri("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void convertRequiresWriteAuthority() {
        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .put().uri("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void convertRequiresUserRole() {
        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void convertAllowsUserWithWriteAuthority() {
        when(convertService.convertCurrency(Currency.USD, Currency.EUR, new BigDecimal("10")))
                .thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлена конвертация 10 EUR в USD"))
                .thenReturn(Mono.empty());

        webTestClient.mutateWith(csrf())
                .mutateWith(mockJwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isOk();
    }

    private String requestBody() {
        return "{\"toCurrency\":\"USD\",\"fromCurrency\":\"EUR\",\"sum\":10}";
    }
}
