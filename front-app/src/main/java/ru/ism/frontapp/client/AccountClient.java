package ru.ism.frontapp.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;
import ru.ism.frontapp.dto.RegisterRequest;
import ru.ism.frontapp.dto.AuthResponse;
import ru.ism.frontapp.web.RegisterForm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class AccountClient {


    private final WebClient webClient;

    public AccountClient(WebClient userWebClient) {
        this.webClient = userWebClient.mutate()
                .baseUrl("http://localhost:8081")
                .build();
        ;
    }


    public Mono<UserAuthResponse> findByLoginForAuth(String login) {
        return webClient.get()
                .uri("/api/users/login/{login}/auth", login)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new IllegalArgumentException("User not found")))
                .bodyToMono(UserAuthResponse.class);
    }

    public Mono<UserResponse> findByLogin(String login) {
        return webClient.get()
                .uri("/api/users/login/{login}", login)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Mono<UserResponse> updateUser(String login, UserUpdateRequest request) {
        return webClient.put()
                .uri("/api/users/{login}", login)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка обновления пользователя")
                                .flatMap(msg -> Mono.error(new IllegalArgumentException(msg))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Сервис аккаунтов недоступен")
                                .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(UserResponse.class);
    }

    public Mono<AccountResponse> createAccount(String userLogin, AccountCreateRequest request) {
        return webClient.post()
                .uri("/api/users/{userLogin}/accounts", userLogin)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка создания счета")
                                .flatMap(msg -> Mono.error(new IllegalArgumentException(msg))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Сервис аккаунтов недоступен")
                                .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(AccountResponse.class);
    }

    public Mono<Void> deleteAccount(String userLogin, Currency currency) {
        return webClient.delete()
                .uri("/api/users/{userLogin}/accounts/{currency}", userLogin, currency)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Ошибка удаления счета")
                                .flatMap(msg -> Mono.error(new IllegalArgumentException(msg))))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("Сервис аккаунтов недоступен")
                                .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(Void.class);
    }

    public Flux<AccountResponse> getUserAccounts(String userLogin) {
        return webClient.get()
                .uri("/api/users/{userLogin}/accounts", userLogin)
                .retrieve()
                .bodyToFlux(AccountResponse.class);
    }

    public record UserResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate
    ) {
    }

    public record UserAuthResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate,
            String password
    ) {
    }

    public record UserUpdateRequest(
            String name,
            LocalDate birthDate,
            String password
    ) {
    }

    public record AccountCreateRequest(
            @NotBlank Currency currency,
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal initialBalance
    ) {
    }

    public record AccountResponse(
            Long id,
            String userLogin,
            String number,
            BigDecimal balance,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

    }
}