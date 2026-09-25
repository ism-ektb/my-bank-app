package ru.ism.mybanktransferapp.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.exception.ValidationException;
import ru.ism.mybankdto.module.TransferRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Модульные тесты сервиса переводов.
 *
 * <p>Внешний аккаунт-сервис заменён HTTP-сервером-заглушкой, чтобы проверить
 * успешные операции, недостаток средств и ошибки внешнего сервиса.</p>
 */
class TransferServiceImplTest {

    public MockWebServer mockAccount;
    NotificationTransferServiceImpl notificationTransferService = Mockito.mock(NotificationTransferServiceImpl.class);
    private TransferServiceImpl transferService;
    private MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);


    /**
     * Запускает HTTP-сервер-заглушку и создаёт тестируемый сервис.
     *
     * @throws IOException если сервер не удалось запустить
     */
    @BeforeEach
    void initialize() throws IOException {
        mockAccount = new MockWebServer();
        mockAccount.start();
        String baseUrl = String.format("http://localhost:%s",
                mockAccount.getPort());
        transferService = new TransferServiceImpl(WebClient.builder().build(), baseUrl, notificationTransferService, meterRegistry);
    }

    /**
     * Останавливает HTTP-сервер-заглушку после каждого теста.
     *
     * @throws IOException если сервер не удалось закрыть
     */
    @AfterEach
    void cleanUp() throws IOException {
        mockAccount.close();
    }

    /**
     * Проверяет успешное выполнение перевода.
     */
    @Test
    void transfer() {
        mockAccount.enqueue(new MockResponse().setResponseCode(200).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).setBody("{\"login\": \"testUser\", \"balance\": 100}"));
        mockAccount.enqueue(new MockResponse().setResponseCode(200));
        when(notificationTransferService.sendNotification(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        transferService.transfer(new TransferRequest("user", 10L), token).block();
        verify(meterRegistry, never()).counter(anyString(), anyString(), anyString());
    }

    /**
     * Проверяет ошибку перевода при недостаточном балансе отправителя.
     */
    @Test
    void transfer_balance_not_enough() {
        mockAccount.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"login\": \"testUser\", \"balance\": 1}"));
        mockAccount.enqueue(new MockResponse().setResponseCode(200));
        when(notificationTransferService.sendNotification(any())).thenReturn(Mono.empty());
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(Mockito.mock(Counter.class));
        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(ValidationException.class, () -> transferService.transfer(
                new TransferRequest("user", 10L), token).block());
        verify(meterRegistry).counter(anyString(), anyString(), anyString());
    }

    /**
     * Проверяет обработку ошибки аккаунт-сервиса.
     */
    @Test
    void transfer_account_service_error() {
        mockAccount.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"login\": \"testUser\", \"balance\": 100}"));
        mockAccount.enqueue(new MockResponse().setResponseCode(400));
        when(notificationTransferService.sendNotification(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(Exception.class, () -> transferService.transfer(
                new TransferRequest("user", 10L), token).block());
        verify(meterRegistry).counter(anyString(), anyString(), anyString());
    }
}