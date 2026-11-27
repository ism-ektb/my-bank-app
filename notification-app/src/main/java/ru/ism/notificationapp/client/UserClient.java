package ru.ism.notificationapp.client;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient() {
        this(RestClient.builder());
    }

    UserClient(RestClient.Builder restClientBuilder) {
        ClientHttpRequestInterceptor bearerForwarder = (request, body, execution) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        };
        this.restClient = restClientBuilder
                .requestInterceptor(bearerForwarder)
                .baseUrl("http://localhost:8081")
                .build();
    }

    public UserResponse getUser(){
        return restClient.get().uri("/api/users/login")
                .retrieve()
                .requiredBody(UserResponse.class);
    }

    public record UserResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ){}
}
