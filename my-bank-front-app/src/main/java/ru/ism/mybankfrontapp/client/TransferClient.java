package ru.ism.mybankfrontapp.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.*;

import java.time.LocalDate;

@Component
public class TransferClient {

    @Autowired
    private WebClient gatewayWebClient;
    private final String gatewayBaseUrl;

    public TransferClient(WebClient gatewayWebClient,
                          @Value("${bank.gateway.base-url}") String gatewayBaseUrl) {
        this.gatewayWebClient = gatewayWebClient;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }


    public Mono<AccountResponseDto> updateAccount(String name, LocalDate birthDate) {
        AccountRequestDto request = new AccountRequestDto(name, birthDate);
        System.out.println(request);
        return gatewayWebClient
                .put()
                .uri(gatewayBaseUrl + "/account")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AccountResponseDto.class);

    }

    public Mono<AccountResponseDto> findAccountByLoginOrCreateForNewLogin() {
        return gatewayWebClient
                .post()
                .uri(gatewayBaseUrl + "/account")
                .retrieve()
                .bodyToMono(AccountResponseDto.class);
    }

    public Mono<Void> cashMoney(long sum, Action action) {
        return gatewayWebClient
                .post()
                .uri(gatewayBaseUrl + "/cash")
                .bodyValue(new ActionDto(sum, action))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> transfer(long amount, String name) {
        return gatewayWebClient
                .post()
                .uri(gatewayBaseUrl + "/transfer")
                .bodyValue(new TransferRequest(name, amount))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Flux<AccountShortResponse> findAllAccounts() {
        return gatewayWebClient
                .get()
                .uri(gatewayBaseUrl + "/account/all")
                .retrieve()
                .bodyToFlux(AccountShortResponse.class);
    }
}
