package ru.ism.transferapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.transferapp.domain.Currency;


import java.math.BigDecimal;

@Component
public class TransferClient {

    private final WebClient webClient;

    public TransferClient() {
        this(WebClient.builder(), "http://localhost:8081/");
    }

    TransferClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder
                .filter(new ServerBearerExchangeFilterFunction())
                .baseUrl(baseUrl)
                .build();
    }



    public Mono<Void> transferBalance(TransferRequest request) {
        return webClient.put()
                .uri("/api/users/accounts/transfer")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка при переводе")
                                .flatMap(message -> Mono.error(new IllegalStateException(message))))
                .bodyToMono(Void.class);
    }

    public record TransferRequest(
            String toLogin,
            Currency currency,
            BigDecimal sum

    ) {

    }
}
