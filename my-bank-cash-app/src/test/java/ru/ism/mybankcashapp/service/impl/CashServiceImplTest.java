package ru.ism.mybankcashapp.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Action;
import ru.ism.mybankdto.module.ActionDto;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashServiceImplTest {

    public MockWebServer mockAccount;
    private CashServiceImpl cashService;
    private final NotificationCashServiceImpl notificationCashService = mock(NotificationCashServiceImpl.class);
    private final MeterRegistry meterRegistry = mock(MeterRegistry.class);

    @BeforeEach
    void initialize() throws IOException {
        mockAccount = new MockWebServer();
        mockAccount.start();

        String baseUrl = String.format("http://localhost:%s",
                mockAccount.getPort());
        cashService = new CashServiceImpl(WebClient.builder().build(), baseUrl, notificationCashService, meterRegistry);
    }

    @AfterEach
    void cleanup() throws IOException {
        mockAccount.close();
    }

    @Test
    void cash_good_response() {
        mockAccount.enqueue(new MockResponse().setResponseCode(200));
        when(notificationCashService.sendNotification(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        cashService.cash(new ActionDto(1L, Action.PUT), token).block();
    }

    @Test
    void cash_400_response() {
        mockAccount.enqueue(new MockResponse().setResponseCode(400));
        when(notificationCashService.sendNotification(any())).thenReturn(Mono.empty());

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(Exception.class, () -> cashService.cash(new ActionDto(1L, Action.PUT), token).block());
        verify(meterRegistry).counter(anyString(), anyString(), anyString());
    }
}