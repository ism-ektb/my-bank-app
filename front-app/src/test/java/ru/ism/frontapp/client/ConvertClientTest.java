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

class ConvertClientTest {

    private StubHttpServer server;
    private ConvertClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new ConvertClient(WebClient.builder().build(), server.baseUrl());
        server.respond("/api/convert", 204, MediaType.APPLICATION_JSON_VALUE, "");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void convertCurrencySendsRequestAndCompletes() {
        var request = new ConvertClient.FrontConvertRequest(
                Currency.USD, Currency.EUR, new BigDecimal("13.50"));

        StepVerifier.create(client.convertCurrency(request)).verifyComplete();

        assertEquals("PUT", server.lastRequest().method());
        assertEquals("/api/convert", server.lastRequest().path());
        assertTrue(server.lastRequest().body().contains("\"toCurrency\":\"USD\""));
        assertTrue(server.lastRequest().body().contains("\"fromCurrency\":\"EUR\""));
        assertTrue(server.lastRequest().body().contains("\"sum\":13.50"));
    }

    @Test
    void convertCurrencyMapsErrorBodyToIllegalStateException() {
        server.respond("/api/convert", HttpStatus.BAD_REQUEST.value(), MediaType.TEXT_PLAIN_VALUE, "Conversion denied");

        StepVerifier.create(client.convertCurrency(new ConvertClient.FrontConvertRequest(
                        Currency.USD, Currency.EUR, BigDecimal.TEN)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Conversion denied"))
                .verify();
    }

    @Test
    void convertCurrencyUsesDefaultMessageForEmptyErrorBody() {
        server.respond("/api/convert", HttpStatus.INTERNAL_SERVER_ERROR.value(), MediaType.TEXT_PLAIN_VALUE, "");

        StepVerifier.create(client.convertCurrency(new ConvertClient.FrontConvertRequest(
                        Currency.USD, Currency.EUR, BigDecimal.TEN)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка конвертации"))
                .verify();
    }
}
