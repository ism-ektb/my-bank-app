package ru.ism.convertapp.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import ru.ism.convertapp.domain.Currency;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountClientTest {

    private HttpServer server;
    private AccountClient accountClient;
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
        server.createContext("/api/users/accounts/convert", exchange -> {
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
        accountClient = new AccountClient(WebClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void convertBalanceSendsPutRequestWithConversionDetails() {
        var request = new AccountClient.ConvertRequest(
                Currency.USD, Currency.EUR, new BigDecimal("10.50"), new BigDecimal("9.25"));

        StepVerifier.create(accountClient.convertBalance(request))
                .verifyComplete();

        assertEquals("PUT", requestMethod.get());
        assertEquals("/api/users/accounts/convert", requestPath.get());
        assertTrue(requestBody.get().contains("\"toCurrency\":\"USD\""));
        assertTrue(requestBody.get().contains("\"fromCurrency\":\"EUR\""));
        assertTrue(requestBody.get().contains("\"toSum\":10.50"));
        assertTrue(requestBody.get().contains("\"fromSum\":9.25"));
    }

    @Test
    void convertBalancePropagatesErrorBody() {
        responseStatus = HttpStatus.BAD_REQUEST.value();
        responseBody = "Conversion rejected";

        StepVerifier.create(accountClient.convertBalance(new AccountClient.ConvertRequest(
                        Currency.USD, Currency.EUR, BigDecimal.TEN, BigDecimal.ONE)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Conversion rejected"))
                .verify();
    }

    @Test
    void convertBalanceUsesDefaultErrorForEmptyErrorBody() {
        responseStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();

        StepVerifier.create(accountClient.convertBalance(new AccountClient.ConvertRequest(
                        Currency.USD, Currency.EUR, BigDecimal.TEN, BigDecimal.ONE)))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ошибка при конвертации"))
                .verify();
    }
}
