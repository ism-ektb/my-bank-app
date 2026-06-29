package ru.ism.mybankfrontapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.*;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {

        XorServerCsrfTokenRequestAttributeHandler xorHandler = new XorServerCsrfTokenRequestAttributeHandler();
        xorHandler.setTokenFromMultipartDataEnabled(true);

        DelegatingServerLogoutHandler logoutHandler = new DelegatingServerLogoutHandler(
                new WebSessionServerLogoutHandler(),
                new SecurityContextServerLogoutHandler()
        );

        OidcClientInitiatedServerLogoutSuccessHandler successHandler = new OidcClientInitiatedServerLogoutSuccessHandler(
                clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}");

        return http
                .authorizeExchange(auth -> auth
                        .pathMatchers("/", "/css/**", "/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieServerCsrfTokenRepository())
                        .csrfTokenRequestHandler(xorHandler))
                .logout(logoutSpec -> logoutSpec
                        .logoutHandler(logoutHandler)
                        .logoutSuccessHandler(logoutSuccess_Handler()))
                .build();
    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccess_Handler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create("/"));
        return handler;
    }
}
