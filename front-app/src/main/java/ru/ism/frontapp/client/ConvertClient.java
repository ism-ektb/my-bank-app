package ru.ism.frontapp.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;

@Component
public class ConvertClient {

    private final WebClient webClient;

    @Autowired
    public ConvertClient(WebClient userWebClient) {
        this(userWebClient, "http://localhost:8084");
    }

    ConvertClient(WebClient userWebClient, String baseUrl) {
        this.webClient = userWebClient.mutate()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<Void> convertCurrency(FrontConvertRequest request) {
        return webClient.put()
                .uri("/api/convert")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка конвертации")
                                .flatMap(message -> Mono.error(new IllegalStateException(message))))
                .bodyToMono(Void.class);
    }

    public record FrontConvertRequest(
            @NotNull Currency toCurrency,
            @NotNull Currency fromCurrency,
            @NotNull BigDecimal sum){}
}
