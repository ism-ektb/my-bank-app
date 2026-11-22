package ru.ism.frontapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.client.AccountClient;

import java.net.URI;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        XorServerCsrfTokenRequestAttributeHandler xorHandler = new XorServerCsrfTokenRequestAttributeHandler();
        xorHandler.setTokenFromMultipartDataEnabled(true);

        ServerAuthenticationSuccessHandler successHandler = (webFilterExchange, authentication) -> {
            var exchange = webFilterExchange.getExchange();

            return exchange.getSession()
                    .doOnNext(session -> {
                        session.getAttributes().put("authSuccessMessage",
                                "Успешная аутентификация для: " + authentication.getName());
                    })
                    .then(Mono.defer(() -> {
                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
                        exchange.getResponse().getHeaders().setLocation(URI.create("/"));
                        return exchange.getResponse().setComplete();
                    }));
        };

        ServerLogoutSuccessHandler logoutSuccessHandler = (exchange, authentication) -> {
            var response = exchange.getExchange().getResponse();

            response.getHeaders().add("Set-Cookie",
                    org.springframework.http.ResponseCookie.from("SESSION", "")
                            .path("/")
                            .maxAge(java.time.Duration.ZERO)
                            .httpOnly(true)
                            .sameSite("Lax")
                            .build()
                            .toString());

            response.getHeaders().add("Set-Cookie",
                    org.springframework.http.ResponseCookie.from("JSESSIONID", "")
                            .path("/")
                            .maxAge(java.time.Duration.ZERO)
                            .httpOnly(true)
                            .sameSite("Lax")
                            .build()
                            .toString());

            return exchange.getExchange().getSession()
                    .doOnNext(session -> session.getAttributes()
                            .put("authLogoutMessage", "Вы успешно вышли из системы"))
                    .then(Mono.defer(() -> {
                        response.setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
                        response.getHeaders().setLocation(URI.create("/"));
                        return response.setComplete();
                    }));
        };


        ServerAuthenticationFailureHandler failureHandler = (webFilterExchange, exception) -> {
            var exchange = webFilterExchange.getExchange();

            return exchange.getSession()
                    .doOnNext(session -> {
                        session.getAttributes().put("authFailureMessage",
                                "Ошибка аутентификации: " + exception.getMessage());
                    })
                    .then(Mono.defer(() -> {
                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
                        exchange.getResponse().getHeaders().setLocation(URI.create("/auth/login?error=1"));
                        return exchange.getResponse().setComplete();
                    }));
        };



        return http
                .csrf(csrfSpec -> csrfSpec.csrfTokenRepository(new CookieServerCsrfTokenRepository())
                        .csrfTokenRequestHandler(xorHandler)
                        .requireCsrfProtectionMatcher(
                                new AndServerWebExchangeMatcher(
                                        // 1. Сохраняем стандартное правило (только PUT, POST, DELETE)
                                        CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                                        // 2. Добавляем исключение для нашего пути (НЕ применять CSRF к /auth/register/**)
                                        new NegatedServerWebExchangeMatcher(
                                                ServerWebExchangeMatchers.pathMatchers("/auth/register/**")
                                        )
                                )))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/register", "/auth/login", "/login", "/css/**", "/js/**", "/test/**").permitAll()
                        .anyExchange().authenticated()
                )
                .logout(logoutSpec -> logoutSpec
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .logoutHandler(new WebSessionServerLogoutHandler())
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                
                .build();
    }
/*
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            AccountClient accountClient,
            PasswordEncoder passwordEncoder
    ) {
        return authentication -> {
            String email = authentication.getName();
            String rawPassword = authentication.getCredentials().toString();

            return accountClient.findByLoginForAuth(email)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Пользователь не найден")))
                    .flatMap(user -> {
                        if (passwordEncoder.matches(rawPassword, user.password())) {
                            return Mono.just(new UsernamePasswordAuthenticationToken(
                                    user.login(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            ));
                        }
                        return Mono.error(new BadCredentialsException("Неверный пароль"));
                    });
        };
    }


 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}