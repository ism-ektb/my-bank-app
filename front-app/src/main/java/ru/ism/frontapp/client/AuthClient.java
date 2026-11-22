package ru.ism.frontapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.dto.RegisterRequest;
import ru.ism.frontapp.web.RegisterForm;

@Component
public class AuthClient {

    private final WebClient webClient;

    public AuthClient(WebClient webClient) {
        this.webClient = webClient.mutate()
                .baseUrl("http://localhost:8081").build();
    }

    public Mono<AccountClient.UserResponse> register(RegisterForm form) {
        return webClient.post()
                .uri("/api/users")
                .bodyValue(new RegisterRequest(
                        form.getName(),
                        form.getLogin(),
                        form.getBirthDate(),
                        new BCryptPasswordEncoder().encode(form.getPassword())
                ))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка регистрации")
                                .flatMap(msg -> Mono.error(new IllegalArgumentException(msg))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Сервис аккаунтов недоступен")
                                .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(AccountClient.UserResponse.class);
    }
}
