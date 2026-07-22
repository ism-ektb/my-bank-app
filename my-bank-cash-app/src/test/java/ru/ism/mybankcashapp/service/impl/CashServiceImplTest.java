package ru.ism.mybankcashapp.service.impl;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ism.mybankdto.module.Action;
import ru.ism.mybankdto.module.ActionDto;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CashServiceImplTest {

    public MockWebServer mockAccount;
    public MockWebServer mockNotification;
    private CashServiceImpl cashService;

    @BeforeEach
    void initialize() throws IOException {
        mockAccount = new MockWebServer();
        mockAccount.start();
        mockNotification = new MockWebServer();
        mockNotification.start();

        String baseUrl = String.format("http://localhost:%s",
                mockAccount.getPort());
        String baseUrl1 = String.format("http://localhost:%s",
                mockNotification.getPort());
        cashService = new CashServiceImpl(WebClient.builder().build(), baseUrl, baseUrl1);
    }

    @AfterEach
    void cleanup() throws IOException {
        mockAccount.close();
        mockNotification.close();
    }

    @Test
    void cash_good_response() {
        mockAccount.enqueue(new MockResponse().setResponseCode(200));
        mockNotification.enqueue(new MockResponse().setResponseCode(200));

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
        mockNotification.enqueue(new MockResponse().setResponseCode(200));

        Jwt jwt = Jwt.withTokenValue("testToken")
                .header("alg", "HS256")
                .claim("preferred_username", "testUser")
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
        assertThrows(Exception.class, () -> cashService.cash(new ActionDto(1L, Action.PUT), token).block());
    }
}