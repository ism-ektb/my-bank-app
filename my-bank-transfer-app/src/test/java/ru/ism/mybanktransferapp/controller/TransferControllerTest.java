package ru.ism.mybanktransferapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.TransferRequest;
import ru.ism.mybanktransferapp.config.SecurityConfig;
import ru.ism.mybanktransferapp.service.TransferService;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = TransferController.class)
@Import(SecurityConfig.class)
class TransferControllerTest {
    @Autowired
    private WebTestClient webClient;
    @MockitoBean
    private TransferService transferService;

    @Test
    void transfer_good_result() {
        when(transferService.transfer(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/transfer")
                .bodyValue(new TransferRequest("test", 1L))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void transfer_sum_equals_zero() {
        when(transferService.transfer(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/transfer")
                .bodyValue(new TransferRequest("test", 0L))
                .exchange()
                .expectStatus().isEqualTo(400);
    }

    @Test
    void transfer_bad_authorities() {
        when(transferService.transfer(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .post()
                .uri("/transfer")
                .bodyValue(new TransferRequest("test", 1L))
                .exchange()
                .expectStatus().isEqualTo(403);
    }
}