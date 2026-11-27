package ru.ism.transferapp.client;

import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient() {
        this(WebClient.builder(), "http://localhost:8087");
    }

    NotificationClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder
                .filter(new ServerBearerExchangeFilterFunction())
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<Void> send(String text){
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/notification")
                        .queryParam("text", text)
                        .build())
                .retrieve()
                .bodyToMono(Void.class);
    }
}
