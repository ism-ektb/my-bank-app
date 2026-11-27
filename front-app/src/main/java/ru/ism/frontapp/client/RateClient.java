package ru.ism.frontapp.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RateClient {

    private final WebClient webClient;

    @Autowired
    public RateClient(WebClient userWebClient) {
        this(userWebClient, "http://localhost:8082");
    }

    RateClient(WebClient userWebClient, String baseUrl) {
        this.webClient = userWebClient.mutate()
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
