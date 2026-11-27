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

class TransferClientTest {

    private StubHttpServer server;
    private TransferClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new TransferClient(WebClient.builder().build(), server.baseUrl());
        server.respond("/api/transfer", 204, MediaType.APPLICATION_JSON_VALUE, "");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void transferBalanceSendsTransferDetails() {
        var request = new TransferClient.TransferRequest("bob", Currency.USD, new BigDecimal("45.00"));

        StepVerifier.create(client.transferBalance(request)).verifyComplete();

        assertEquals("PUT", server.lastRequest().method());
        assertEquals("/api/transfer", server.lastRequest().path());
        assertTrue(server.lastRequest().body().contains("\"toLogin\":\"bob\""));
        assertTrue(server.lastRequest().body().contains("\"currency\":\"USD\""));
        assertTrue(server.lastRequest().body().contains("\"sum\":45.00"));
    }

    @Test
    void transferBalanceMapsErrorBodyToIllegalStateException() {
        server.respond("/api/transfer", HttpStatus.BAD_REQUEST.value(), MediaType.TEXT_PLAIN_VALUE, "Transfer denied");

        StepVerifier.create(client.transferBalance(new TransferClient.TransferRequest("bob", Currency.EUR, BigDecimal.TEN)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Transfer denied"))
                .verify();
    }

    @Test
    void transferBalanceUsesDefaultMessageForEmptyErrorBody() {
        server.respond("/api/transfer", HttpStatus.INTERNAL_SERVER_ERROR.value(), MediaType.TEXT_PLAIN_VALUE, "");

        StepVerifier.create(client.transferBalance(new TransferClient.TransferRequest("bob", Currency.EUR, BigDecimal.TEN)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка при переводе средств"))
                .verify();
    }
}
