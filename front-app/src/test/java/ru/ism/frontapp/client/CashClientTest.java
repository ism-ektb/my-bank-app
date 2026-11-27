package ru.ism.frontapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashClientTest {

    private StubHttpServer server;
    private CashClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new CashClient(WebClient.builder().build(), server.baseUrl());
        server.respond("/api/add", 204, MediaType.APPLICATION_JSON_VALUE, "");
        server.respond("/api/reduce", 204, MediaType.APPLICATION_JSON_VALUE, "");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void addBalanceSendsCurrencyAndDelta() {
        StepVerifier.create(client.addBalance(Currency.RUB, new BigDecimal("20.50"))).verifyComplete();

        assertEquals("PUT", server.lastRequest().method());
        assertEquals("/api/add", server.lastRequest().path());
        assertTrue(server.lastRequest().body().contains("\"delta\":20.50"));
        assertTrue(server.lastRequest().body().contains("\"currency\":\"RUB\""));
    }

    @Test
    void reduceBalanceSendsReduceRequest() {
        StepVerifier.create(client.reduceBalance(Currency.USD, new BigDecimal("8"))).verifyComplete();

        assertEquals("PUT", server.lastRequest().method());
        assertEquals("/api/reduce", server.lastRequest().path());
        assertTrue(server.lastRequest().body().contains("\"currency\":\"USD\""));
    }

    @Test
    void addBalanceMapsErrorBodyToIllegalStateException() {
        server.respond("/api/add", HttpStatus.BAD_REQUEST.value(), MediaType.TEXT_PLAIN_VALUE, "Not enough funds");

        StepVerifier.create(client.addBalance(Currency.EUR, BigDecimal.ONE))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Not enough funds"))
                .verify();
    }

    @Test
    void reduceBalanceUsesDefaultMessageForEmptyErrorBody() {
        server.respond("/api/reduce", HttpStatus.INTERNAL_SERVER_ERROR.value(), MediaType.TEXT_PLAIN_VALUE, "");

        StepVerifier.create(client.reduceBalance(Currency.EUR, BigDecimal.ONE))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка снятия средств"))
                .verify();
    }
}
