package ru.ism.frontapp.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.domain.Currency;
import ru.ism.frontapp.validation.MinAge;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountClient {


    private final WebClient webClient;
    private final Cache<String, Mono<UserResponse>> userCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .build();
    private final Cache<String, Mono<List<UserResponse>>> otherUserCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .build();

    @Autowired
    public AccountClient(WebClient userWebClient) {
        this(userWebClient, "http://localhost:8081");
    }

    AccountClient(WebClient userWebClient, String baseUrl) {
        this.webClient = userWebClient.mutate()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<UserResponse> findByLogin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName())
                .flatMap(login -> {
                    Mono<UserResponse> cached = userCache.getIfPresent(login);
                    if (cached == null) {
                        cached = webClient.get()
                                .uri("/api/users/login")
                                .retrieve()
                                .bodyToMono(UserResponse.class)
                                .cache(Duration.ofMinutes(5));
                        userCache.put(login, cached);
                    }
                    return cached;
                });
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
                .bodyToMono(UserResponse.class)
                .doOnSuccess(user -> {
                    userCache.invalidateAll();
                    otherUserCache.invalidateAll();
                });
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



    public Flux<UserResponse> findOtherUserNames() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName())
                .flatMapMany(login -> {
                    Mono<List<UserResponse>> cached = otherUserCache.getIfPresent(login);
                    if (cached == null) {
                        cached = webClient.get()
                                .uri("/api/users/names/others")
                                .retrieve()
                                .bodyToFlux(UserResponse.class)
                                .collectList()
                                .cache(Duration.ofMinutes(5));
                        otherUserCache.put(login, cached);
                    }
                    return cached.flatMapMany(Flux::fromIterable);
                });
    }

    public Flux<AccountResponse> getUserAccounts() {
        return  webClient.get()
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