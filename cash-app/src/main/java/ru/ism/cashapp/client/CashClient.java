package ru.ism.cashapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class CashClient {

    private final WebClient webClient;

    public CashClient() {
        this(WebClient.builder(), "http://localhost:8081/");
    }

    CashClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder
                .filter(new ServerBearerExchangeFilterFunction())
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<Void> addBalance(String currency, BigDecimal delta) {
        return changeBalance("/api/users/accounts/add", currency, delta, "Ошибка пополнения счета");
    }

    public Mono<Void> reduceBalance(String currency, BigDecimal delta) {
        return changeBalance("/api/users/accounts/reduce", currency, delta, "Ошибка уменьшения баланса счета");
    }

    private Mono<Void> changeBalance(String path, String currency, BigDecimal delta, String errorMessage) {
        return webClient.put()
                .uri(path)
                .bodyValue(new BalanceChangeRequest(delta, currency))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty(errorMessage)
                                .flatMap(message -> Mono.error(new IllegalStateException(message))))
                .bodyToMono(Void.class);
    }

    public record BalanceChangeRequest(BigDecimal delta, String currency) {
    }
}
