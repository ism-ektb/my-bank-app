package ru.ism.mybankfrontapp.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.AccountShortResponse;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("contract-test")
@AutoConfigureStubRunner(
        ids = "my-bank-app:my-bank-account-app:+:stubs:8081",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class TransferClientTest {


    private TransferClient transferClient;
    @Value("${bank.gateway.base-url}")
    private String gatewayBaseUrl;


    @Test
    void findAllAccounts() {
        transferClient = new TransferClient(WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer **")
                .build(),
                gatewayBaseUrl);
        AccountShortResponse dto = transferClient.findAllAccounts().blockFirst();
        assertNotNull(dto);
        assertEquals(new AccountShortResponse("testUser1", "testUser1"), dto);
    }

    @Test
    void getAccount() {
        transferClient = new TransferClient(WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer **")
                .build(),
                gatewayBaseUrl);
        AccountResponseDto dto = transferClient.findAccountByLoginOrCreateForNewLogin().block();
        assertNotNull(dto);
        assertEquals(new AccountResponseDto("testUser", LocalDate.of(1999, 1, 1), "testUser", 10L), dto);
    }
}