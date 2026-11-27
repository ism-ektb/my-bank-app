package ru.ism.convertapp.client;

import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RateClient {

    private final WebClient webClient;

    public RateClient() {
        this(WebClient.builder(), "http://localhost:8082");
    }

    RateClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder
                .filter(new ServerBearerExchangeFilterFunction())
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<Float> getEuroRate() {
        return webClient.get()
                .uri("/rate/euro")
                .retrieve()
                .bodyToMono(Float.class);
    }

    public Mono<Float> getDollarRate() {
        return webClient.get()
                .uri("/rate/dollar")
                .retrieve()
                .bodyToMono(Float.class);
    }
}
