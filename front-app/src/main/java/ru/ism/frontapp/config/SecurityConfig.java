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
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
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
                        .csrfTokenRequestHandler(xorHandler))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/register", "/auth/login", "/login", "/css/**", "/js/**", "/test/**").permitAll()
                        .anyExchange().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .authenticationSuccessHandler(successHandler)
                        .authenticationFailureHandler((webFilterExchange, exception) -> {
                            var exchange = webFilterExchange.getExchange();

                            String message = "Неверный логин или пароль";
                            String encoded = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
                            exchange.getResponse().getHeaders().setLocation(URI.create("/auth/login?error=" + encoded));
                            return exchange.getResponse().setComplete();
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((exchange, authentication) -> {
                            exchange.getExchange().getSession()
                                    .doOnNext(session -> session.getAttributes()
                                            .put("authLogoutMessage", "Вы успешно вышли"))
                                    .subscribe();

                            exchange.getExchange().getResponse().setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
                            exchange.getExchange().getResponse().getHeaders().setLocation(URI.create("/auth/login?logout=1"));
                            return exchange.getExchange().getResponse().setComplete();
                        })
                )
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}