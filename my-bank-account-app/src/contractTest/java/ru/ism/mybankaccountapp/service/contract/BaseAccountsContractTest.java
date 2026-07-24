package ru.ism.mybankaccountapp.service.contract;

import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankaccountapp.controller.AccountController;
import ru.ism.mybankaccountapp.service.AccountService;
import ru.ism.mybankdto.module.AccountResponseDto;
import ru.ism.mybankdto.module.AccountShortResponse;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@WebFluxTest(controllers = AccountController.class)
@ActiveProfiles("contract-test")
public abstract class BaseAccountsContractTest {

    @MockitoBean
    protected AccountService accountService;

    @BeforeEach
    void setup() {
        RestAssuredWebTestClient.standaloneSetup(new AccountController(accountService));
        when(accountService.findAllWithoutUser(any()))
                .thenReturn(Flux.just(new AccountShortResponse("testUser1", "testUser1")));
        when(accountService.findAccount(any()))
                .thenReturn(Mono.just(new AccountResponseDto("testUser",
                        LocalDate.of(1999, 1, 1), "testUser", 10L)));
    }
}
