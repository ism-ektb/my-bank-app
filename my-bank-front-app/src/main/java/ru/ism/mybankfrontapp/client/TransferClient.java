package ru.ism.mybankfrontapp.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ism.mybankfrontapp.model.AccountRequestDto;
import ru.ism.mybankfrontapp.model.AccountResponseDto;

import java.time.LocalDate;

@Component
public class TransferClient {

    private final WebClient gatewayWebClient;
    private final String gatewayBaseUrl;

    public TransferClient(WebClient gatewayWebClient,
                          @Value("${bank.gateway.base-url}") String gatewayBaseUrl) {
        this.gatewayWebClient = gatewayWebClient;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }


    public AccountResponseDto updateAccount(String name, LocalDate birthDate) {
        AccountRequestDto request = new AccountRequestDto(name, birthDate);
        System.out.println(request);
        return gatewayWebClient
                .put()
                .uri(gatewayBaseUrl + "/account")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AccountResponseDto.class).block();

    }

    public AccountResponseDto findAccountByLogin() {
        return gatewayWebClient
                .get()
                .uri(gatewayBaseUrl + "/account")
                .retrieve()
                .bodyToMono(AccountResponseDto.class).block();
    }
}
