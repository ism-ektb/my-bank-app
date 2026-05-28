package ru.ism.mybankfrontapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public WebClientConfig(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public WebClient gatewayWebClient() {
        return WebClient.builder()
                .filter(addAccessTokenHeader())
                .build();
    }

    /**
     * Фильтр для передачи JWT-токена пользователя в Gateway API
     * Извлекает Access Token из OAuth2AuthorizedClient и добавляет его в заголовок Authorization
     * Access Token содержит информацию о пользователе, ролях и правах, необходимую для Resource Server
     */
    private ExchangeFilterFunction addAccessTokenHeader() {
        return (request, next) -> {
            // Достаём текущую аутентификацию из SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Проверяем, что пользователь залогинен через OAuth2 (OAuth2AuthenticationToken)
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                // Из токена берём clientRegistrationId (имя клиента в настройках security)
                // и имя пользователя (principal), чтобы найти его authorized client
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        oauth2Token.getAuthorizedClientRegistrationId(),
                        oauth2Token.getName()
                );

                // Если для этого пользователя и клиента нашли authorized client —
                // пробуем достать из него access token (JWT)
                OAuth2AccessToken accessToken = authorizedClient != null ? authorizedClient.getAccessToken() : null;

                // Если access token есть, добавляем его в заголовок Authorization
                if (accessToken != null) {
                    ClientRequest requestWithToken = ClientRequest.from(request)
                            .header("Authorization", "Bearer " + accessToken.getTokenValue())
                            .build();

                    // Отправляем уже изменённый запрос дальше по цепочке фильтров WebClient
                    return next.exchange(requestWithToken);
                }
            }

            // Если аутентификация не OAuth2 или токен не нашли —
            // отправляем исходный запрос без изменений
            return next.exchange(request);
        };
    }
}
