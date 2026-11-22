package ru.ism.frontapp.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;
import ru.ism.frontapp.validation.MinAge;

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
    }

    public Mono<UserResponse> findByLogin() {
        return webClient.get()
                .uri("/api/users/login")
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    public Mono<UserResponse> updateUser(UserUpdateRequest request) {
        return webClient.put()
                .uri("/api/users")
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

    public Mono<AccountResponse> createAccount(AccountCreateRequest request) {
        return webClient.post()
                .uri("/api/users/accounts")
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

    public Mono<Void> deleteAccount(Currency currency) {
        return webClient.delete()
                .uri("/api/users/accounts/{currency}", currency)
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

    @PreAuthorize("hasRole('USER')")
    public Flux<AccountResponse> getUserAccounts() {
        return webClient.get()
                .uri("/api/users/accounts")
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

    public record UserUpdateRequest(
            String name,
            @MinAge(18) LocalDate birthDate,
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