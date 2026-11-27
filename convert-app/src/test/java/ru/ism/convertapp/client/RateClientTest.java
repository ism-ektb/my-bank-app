package ru.ism.convertapp.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateClientTest {

    private HttpServer server;
    private RateClient rateClient;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseBody = "";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rate/", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(responseStatus, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        rateClient = new RateClient(WebClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getEuroRateRequestsEuroEndpointAndParsesResponse() {
        responseBody = "90.25";

        StepVerifier.create(rateClient.getEuroRate())
                .expectNext(90.25f)
                .verifyComplete();

        assertEquals("GET", requestMethod.get());
        assertEquals("/rate/euro", requestPath.get());
    }

    @Test
    void getDollarRateRequestsDollarEndpointAndParsesResponse() {
        responseBody = "81.75";

        StepVerifier.create(rateClient.getDollarRate())
                .expectNext(81.75f)
                .verifyComplete();

        assertEquals("GET", requestMethod.get());
        assertEquals("/rate/dollar", requestPath.get());
    }

    @Test
    void getRatePropagatesServerError() {
        responseStatus = HttpStatus.SERVICE_UNAVAILABLE.value();

        StepVerifier.create(rateClient.getEuroRate())
                .expectErrorMatches(error -> error instanceof org.springframework.web.reactive.function.client.WebClientResponseException
                        && ((org.springframework.web.reactive.function.client.WebClientResponseException) error)
                        .getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value())
                .verify();
    }
}
