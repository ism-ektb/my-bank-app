package ru.ism.cashapp.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationClientTest {

    private HttpServer server;
    private NotificationClient notificationClient;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestText = new AtomicReference<>();
    private int responseStatus;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 204;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/notification", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestText.set(exchange.getRequestURI().getQuery());
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        notificationClient = new NotificationClient(
                WebClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendPostsNotificationTextAsQueryParameter() {
        StepVerifier.create(notificationClient.send("Cash topped up 10 RUB"))
                .verifyComplete();

        assertEquals("POST", requestMethod.get());
        assertEquals("/api/notification", requestPath.get());
        assertEquals("text=Cash topped up 10 RUB", requestText.get());
    }

    @Test
    void sendPropagatesServerError() {
        responseStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();

        StepVerifier.create(notificationClient.send("notification text"))
                .expectErrorMatches(error -> error instanceof org.springframework.web.reactive.function.client.WebClientResponseException
                        && ((org.springframework.web.reactive.function.client.WebClientResponseException) error)
                        .getStatusCode().value() == HttpStatus.INTERNAL_SERVER_ERROR.value())
                .verify();
    }
}
