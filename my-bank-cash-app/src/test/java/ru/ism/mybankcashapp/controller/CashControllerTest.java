package ru.ism.mybankcashapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankcashapp.config.SecurityConfig;
import ru.ism.mybankcashapp.service.CashService;
import ru.ism.mybankdto.module.Action;
import ru.ism.mybankdto.module.ActionDto;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = CashController.class)
@Import(SecurityConfig.class)
class CashControllerTest {
    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private CashService cashService;

    @Test
    void cash_good_result() {
        when(cashService.cash(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/cash")
                .bodyValue(new ActionDto(1L, Action.GET))
                .exchange().expectStatus().isOk();
    }

    @Test
    void cash_bad_dto_sum_0() {
        when(cashService.cash(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/cash")
                .bodyValue(new ActionDto(0L, Action.GET))
                .exchange().expectStatus().isEqualTo(400);
    }

    @Test
    void cash_bad_authorities() {
        when(cashService.cash(any(), any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")
                        ))
                .post()
                .uri("/cash")
                .bodyValue(new ActionDto(1L, Action.GET))
                .exchange().expectStatus().isEqualTo(403);
    }
}