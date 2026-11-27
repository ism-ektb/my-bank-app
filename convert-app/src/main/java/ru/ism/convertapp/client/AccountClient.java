package ru.ism.convertapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.convertapp.domain.Currency;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final WebClient webClient;

    public AccountClient() {
        this(WebClient.builder(), "http://localhost:8081/");
    }

    AccountClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder
                .filter(new ServerBearerExchangeFilterFunction())
                .baseUrl(baseUrl)
                .build();
    }



    public Mono<Void> convertBalance(ConvertRequest request) {
        return webClient.put()
                .uri("/api/users/accounts/convert")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка при конвертации")
                                .flatMap(message -> Mono.error(new IllegalStateException(message))))
                .bodyToMono(Void.class);
    }

    public record ConvertRequest(
            Currency toCurrency,
            Currency fromCurrency,
            BigDecimal toSum,
            BigDecimal fromSum
    ) {

    }
}
