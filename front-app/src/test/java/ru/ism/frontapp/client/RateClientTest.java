package ru.ism.frontapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateClientTest {

    private StubHttpServer server;
    private RateClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new RateClient(WebClient.builder().build(), server.baseUrl());
        server.respond("/rate/euro", 200, MediaType.APPLICATION_JSON_VALUE, "90.25");
        server.respond("/rate/dollar", 200, MediaType.APPLICATION_JSON_VALUE, "81.75");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void getEuroRateGetsEuroEndpoint() {
        StepVerifier.create(client.getEuroRate())
                .expectNext(90.25f)
                .verifyComplete();

        assertEquals("GET", server.lastRequest().method());
        assertEquals("/rate/euro", server.lastRequest().path());
    }

    @Test
    void getDollarRateGetsDollarEndpoint() {
        StepVerifier.create(client.getDollarRate())
                .expectNext(81.75f)
                .verifyComplete();

        assertEquals("GET", server.lastRequest().method());
        assertEquals("/rate/dollar", server.lastRequest().path());
    }

    @Test
    void getRatePropagatesServerError() {
        server.respond("/rate/euro", HttpStatus.SERVICE_UNAVAILABLE.value(), MediaType.TEXT_PLAIN_VALUE, "Unavailable");

        StepVerifier.create(client.getEuroRate())
                .expectErrorMatches(error -> error instanceof org.springframework.web.reactive.function.client.WebClientResponseException
                        && ((org.springframework.web.reactive.function.client.WebClientResponseException) error)
                        .getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value())
                .verify();
    }
}
