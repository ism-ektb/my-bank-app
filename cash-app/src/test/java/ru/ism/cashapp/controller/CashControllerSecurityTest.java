package ru.ism.cashapp.controller;

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
import ru.ism.cashapp.client.CashClient;
import ru.ism.cashapp.client.NotificationClient;
import ru.ism.cashapp.config.CashSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(CashController.class)
@Import(CashSecurityConfig.class)
class CashControllerSecurityTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @MockitoBean
    private CashClient cashClient;

    @MockitoBean
    private NotificationClient notificationClient;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void addBalanceRequiresAuthentication() {
        webTestClient.put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void reduceBalanceRequiresAuthentication() {
        webTestClient.put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void addBalanceRequiresWriteAuthority() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void reduceBalanceRequiresUserRole() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void addBalanceAllowsAuthorizedUser() {
        when(cashClient.addBalance(any(), any())).thenReturn(Mono.empty());
        when(notificationClient.send(any())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockJwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void reduceBalanceAllowsAuthorizedUser() {
        when(cashClient.reduceBalance(any(), any())).thenReturn(Mono.empty());
        when(notificationClient.send(any())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockJwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("account_write")))
                .put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().isOk();
    }
}
