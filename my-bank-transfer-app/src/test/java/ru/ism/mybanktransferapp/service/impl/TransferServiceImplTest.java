package ru.ism.mybanktransferapp.service.impl;

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

class TransferServiceImplTest {

    public MockWebServer mockAccount;
    NotificationTransferServiceImpl notificationTransferService = Mockito.mock(NotificationTransferServiceImpl.class);
    private TransferServiceImpl transferService;



    @BeforeEach
    void initialize() throws IOException {
        mockAccount = new MockWebServer();
        mockAccount.start();
        String baseUrl = String.format("http://localhost:%s",
                mockAccount.getPort());
        transferService = new TransferServiceImpl(WebClient.builder().build(), baseUrl, notificationTransferService);
    }

    @AfterEach
    void cleanUp() throws IOException {
        mockAccount.close();
    }

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
    }

    @Test
    void transfer_balance_not_enough() {
        mockAccount.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"login\": \"testUser\", \"balance\": 1}"));
        mockAccount.enqueue(new MockResponse().setResponseCode(200));
        when(notificationTransferService.sendNotification(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(ValidationException.class, () -> transferService.transfer(
                new TransferRequest("user", 10L), token).block());
    }

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
    }
}