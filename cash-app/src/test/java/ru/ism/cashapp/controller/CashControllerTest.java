package ru.ism.cashapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.ism.cashapp.client.CashClient;
import ru.ism.cashapp.client.NotificationClient;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class CashControllerTest {

    private CashClient cashClient;
    private NotificationClient notificationClient;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        cashClient = mock(CashClient.class);
        notificationClient = mock(NotificationClient.class);
        CashController controller = new CashController(cashClient, notificationClient);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void addBalanceReturnsOkAndSendsNotification() {
        when(cashClient.addBalance("RUB", new BigDecimal("100.50"))).thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлено пополнение счета на сумму 100.50 RUB"))
                .thenReturn(Mono.empty());

        webTestClient.put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":100.50}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(cashClient).addBalance("RUB", new BigDecimal("100.50"));
        verify(notificationClient).send("Осуществлено пополнение счета на сумму 100.50 RUB");
    }

    @Test
    void reduceBalanceReturnsOkAndSendsNotification() {
        when(cashClient.reduceBalance("USD", new BigDecimal("25.00"))).thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлено снятие со счета на сумму 25.00 USD"))
                .thenReturn(Mono.empty());

        webTestClient.put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"USD\",\"delta\":25.00}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(cashClient).reduceBalance("USD", new BigDecimal("25.00"));
        verify(notificationClient).send("Осуществлено снятие со счета на сумму 25.00 USD");
    }

    @Test
    void addBalanceIgnoresNotificationFailure() {
        when(cashClient.addBalance("EUR", new BigDecimal("10"))).thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлено пополнение счета на сумму 10 EUR"))
                .thenReturn(Mono.error(new IllegalStateException("notification unavailable")));

        webTestClient.put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"EUR\",\"delta\":10}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(notificationClient).send("Осуществлено пополнение счета на сумму 10 EUR");
    }

    @Test
    void reduceBalanceIgnoresNotificationFailure() {
        when(cashClient.reduceBalance("EUR", new BigDecimal("7"))).thenReturn(Mono.empty());
        when(notificationClient.send("Осуществлено снятие со счета на сумму 7 EUR"))
                .thenReturn(Mono.error(new IllegalStateException("notification unavailable")));

        webTestClient.put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"EUR\",\"delta\":7}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(notificationClient).send("Осуществлено снятие со счета на сумму 7 EUR");
    }

    @Test
    void addBalancePropagatesCashClientError() {
        when(cashClient.addBalance("RUB", new BigDecimal("10")))
                .thenReturn(Mono.error(new IllegalStateException("Ошибка пополнения счета")));
        when(notificationClient.send("Осуществлено пополнение счета на сумму 10 RUB"))
                .thenReturn(Mono.empty());

        webTestClient.put().uri("/api/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(notificationClient).send("Осуществлено пополнение счета на сумму 10 RUB");
    }

    @Test
    void reduceBalancePropagatesCashClientError() {
        when(cashClient.reduceBalance("RUB", new BigDecimal("10")))
                .thenReturn(Mono.error(new IllegalStateException("Ошибка уменьшения баланса счета")));
        when(notificationClient.send("Осуществлено снятие со счета на сумму 10 RUB"))
                .thenReturn(Mono.empty());

        webTestClient.put().uri("/api/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"currency\":\"RUB\",\"delta\":10}")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(notificationClient).send("Осуществлено снятие со счета на сумму 10 RUB");
    }
}
