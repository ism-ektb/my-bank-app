package ru.ism.transferapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationClientTest {

    private StubHttpServer server;
    private NotificationClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new NotificationClient(WebClient.builder(), server.baseUrl());
        server.respond("/api/notification", 204, "application/json", "");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void sendPostsNotificationTextInQueryParameter() {
        StepVerifier.create(client.send("Transfer complete 10 USD"))
                .verifyComplete();

        assertEquals("POST", server.lastRequest().method());
        assertEquals("/api/notification", server.lastRequest().path());
        assertEquals("text=Transfer%20complete%2010%20USD", server.lastRequest().query());
    }

    @Test
    void sendPropagatesHttpError() {
        server.respond("/api/notification", HttpStatus.INTERNAL_SERVER_ERROR.value(), "text/plain", "Unavailable");

        StepVerifier.create(client.send("notification text"))
                .expectErrorMatches(error -> error instanceof org.springframework.web.reactive.function.client.WebClientResponseException
                        && ((org.springframework.web.reactive.function.client.WebClientResponseException) error)
                        .getStatusCode().value() == HttpStatus.INTERNAL_SERVER_ERROR.value())
                .verify();
    }
}
