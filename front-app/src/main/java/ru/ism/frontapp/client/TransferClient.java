package ru.ism.frontapp.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;

@Component
public class TransferClient {
    private final WebClient webClient;

    @Autowired
    public TransferClient(WebClient webClient) {
        this(webClient, "http://localhost:8086/");
    }

    TransferClient(WebClient webClient, String baseUrl) {
        this.webClient = webClient.mutate()
                .baseUrl(baseUrl).build();
    }

    public Mono<Void> transferBalance(TransferRequest request) {
        return webClient.put()
                .uri("api/transfer")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка при переводе средств")
                                .flatMap(message -> Mono.error(new IllegalStateException(message))))
                .bodyToMono(Void.class);
    }

    public record BalanceChangeRequest(BigDecimal delta, Currency currency) {
    }

    public record TransferRequest(
            @NotNull String toLogin,
            @NotNull Currency currency,
            @NotNull @Positive BigDecimal sum
    ) {
    }
}
