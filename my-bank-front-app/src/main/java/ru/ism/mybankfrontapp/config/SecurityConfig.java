package ru.ism.mybankfrontapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcBackChannelLogoutHandler;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;

import java.net.URI;
import java.net.URL;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {

        XorServerCsrfTokenRequestAttributeHandler xorHandler = new XorServerCsrfTokenRequestAttributeHandler();
        xorHandler.setTokenFromMultipartDataEnabled(true);

        return http
                // Блок настройки авторизации запросов
                .authorizeExchange(auth -> auth
                        // Разрешаем всем доступ к главной странице и статическим ресурсам по пути /css/**
                        .pathMatchers("/", "/css/**", "/actuator/**").permitAll()
                        // Все остальные запросы требуют аутентификации
                        .anyExchange().authenticated()
                )
                // Включаем аутентификацию через OAuth2 Login
                // Неавторизованный пользователь будет перенаправлен на страницу логина провайдера
                .oauth2Login(Customizer.withDefaults())
          //      .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieServerCsrfTokenRepository())
                        .csrfTokenRequestHandler(xorHandler))
                .logout(logoutSpec -> logoutSpec.logoutSuccessHandler(logoutSuccessHandler()))
                .build();

        // Строим и возвращаем цепочку фильтров безопасности

    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccessHandler() {
        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
        return oidcLogoutSuccessHandler;
    }

}
