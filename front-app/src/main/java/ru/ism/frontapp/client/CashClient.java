package ru.ism.frontapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;

@Component
public class CashClient {

    private final WebClient webClient;

    @Autowired
    public CashClient(WebClient webClient) {
        this(webClient, "http://localhost:8083/");
    }

    CashClient(WebClient webClient, String baseUrl) {
        this.webClient = webClient.mutate()
                .baseUrl(baseUrl).build();
    }

    public Mono<Void> addBalance(Currency currency, BigDecimal delta) {
        return changeBalance("/api/add", currency, delta, "Ошибка пополнения счета");
    }

    public Mono<Void> reduceBalance(Currency currency, BigDecimal delta) {
        return changeBalance("/api/reduce", currency, delta, "Ошибка снятия средств");
    }

    private Mono<Void> changeBalance(String path, Currency currency, BigDecimal delta, String errorMessage) {
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

    public record BalanceChangeRequest(BigDecimal delta, Currency currency) {
    }
}
