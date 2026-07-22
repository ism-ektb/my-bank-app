package ru.ism.mybankaccountapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.config.SecurityConfig;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankdto.module.AccountRequestDto;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.CashMoney;
import ru.ism.mybankdto.module.Transfer;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private WebTestClient webClient;
    @MockitoBean
    private AccountService accountService;


    @Test
    @WithMockUser()
    void findByName() {
        when(accountService.findByName(anyString())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.get().uri("/account/qewop").exchange().expectStatus().isOk();
        verify(accountService, times(1)).findByName(anyString());
    }

    @Test
    void credit_good_result() {
        when(accountService.reduceSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .put()
                .uri("/account/credit")
                .bodyValue(new CashMoney("log", 1L))
                .exchange().expectStatus().isOk();
    }

    @Test
    void credit_no_role() {
        when(accountService.reduceSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .put()
                .uri("/account/credit")
                .bodyValue(new CashMoney("log", 1L))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void credit_negative_sum() {
        when(accountService.reduceSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .put()
                .uri("/account/credit")
                .bodyValue(new CashMoney("log", -1L))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void debit_good_result() {
        when(accountService.addSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 1, 1), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .put()
                .uri("/account/debit")
                .bodyValue(new CashMoney("log", 1L))
                .exchange().expectStatus().isOk();
    }

    @Test
    void debit_no_role() {
        when(accountService.addSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .put()
                .uri("/account/debit")
                .bodyValue(new CashMoney("log", 1L))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void debit_negative_sum() {
        when(accountService.addSum(any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .put()
                .uri("/account/debit")
                .bodyValue(new CashMoney("log", -1L))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void update_good_result() {
        when(accountService.updateAccount(any(), any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("account.write")))
                .put()
                .uri("/account")
                .bodyValue(new AccountRequestDto("a", null))
                .exchange().expectStatus().isOk();
    }

    @Test
    void update_no_role() {
        when(accountService.updateAccount(any(), any())).thenReturn(Mono.just(new AccountResponseDto("_", LocalDate.of(1999, 01, 01), "", 1L)));
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .put()
                .uri("/account")
                .bodyValue(new AccountRequestDto("a", null))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void transfer_good_result() {
        when(accountService.transfer(any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/account/transfer")
                .bodyValue(new Transfer("uu", "uu1", 1L))
                .exchange().expectStatus().isOk();
    }

    @Test
    void transfer_no_role() {
        when(accountService.transfer(any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                .post()
                .uri("/account/transfer")
                .bodyValue(new Transfer("uu", "uu1", 1L))
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void transfer_negative_sum() {
        when(accountService.transfer(any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/account/transfer")
                .bodyValue(new Transfer("uu", "uu1", -1L))
                .exchange().expectStatus().is4xxClientError();
    }
    @Test
    void transfer_sender_equal_receiver() {
        when(accountService.transfer(any())).thenReturn(Mono.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .post()
                .uri("/account/transfer")
                .bodyValue(new Transfer("uu", "uu", 1L))
                .exchange().expectStatus().isEqualTo(400);
    }


    @Test
    void getAllAccounts() {
        when(accountService.findAllWithoutUser(any())).thenReturn(Flux.empty());
        webClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("account.write")))
                .get()
                .uri("/account/all")
                .exchange().expectStatus().isOk();
    }
}