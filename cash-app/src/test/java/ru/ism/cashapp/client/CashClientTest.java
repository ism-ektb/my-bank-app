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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashClientTest {

    private HttpServer server;
    private CashClient cashClient;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 204;
        responseBody = "";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        cashClient = new CashClient(WebClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void addBalanceSendsPutRequestWithCurrencyAndDelta() {
        StepVerifier.create(cashClient.addBalance("USD", new java.math.BigDecimal("12.50")))
                .verifyComplete();

        assertEquals("PUT", requestMethod.get());
        assertEquals("/api/users/accounts/add", requestPath.get());
        assertTrue(requestBody.get().contains("\"delta\":12.50"));
        assertTrue(requestBody.get().contains("\"currency\":\"USD\""));
    }

    @Test
    void reduceBalanceSendsPutRequestToReduceEndpoint() {
        StepVerifier.create(cashClient.reduceBalance("EUR", new java.math.BigDecimal("7")))
                .verifyComplete();

        assertEquals("PUT", requestMethod.get());
        assertEquals("/api/users/accounts/reduce", requestPath.get());
        assertTrue(requestBody.get().contains("\"delta\":7"));
        assertTrue(requestBody.get().contains("\"currency\":\"EUR\""));
    }

    @Test
    void addBalancePropagatesErrorBodyAsIllegalStateException() {
        responseStatus = HttpStatus.BAD_REQUEST.value();
        responseBody = "Insufficient funds";

        StepVerifier.create(cashClient.addBalance("RUB", new java.math.BigDecimal("5")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Insufficient funds"))
                .verify();
    }

    @Test
    void reduceBalanceUsesDefaultErrorWhenResponseBodyIsEmpty() {
        responseStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        responseBody = "";

        StepVerifier.create(cashClient.reduceBalance("EUR", new java.math.BigDecimal("1")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка уменьшения баланса счета"))
                .verify();
    }

    @Test
    void addBalanceUsesDefaultErrorWhenResponseBodyIsEmpty() {
        responseStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        responseBody = "";

        StepVerifier.create(cashClient.addBalance("USD", new java.math.BigDecimal("1")))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка пополнения счета"))
                .verify();
    }
}
