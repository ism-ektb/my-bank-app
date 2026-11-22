package ru.ism.accountapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(withDefaults()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)


                .securityContextRepository(new WebSessionServerSecurityContextRepository());

        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter authenticationConverter() {
        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new ReactiveJwtGrantedAuthoritiesConverterAdapter(this::extractAuthorities));
        return jwtConverter;
    }

    private static final Map<String, String> SCOPE_AUTHORITIES = Map.of(
            "read", "account_read",
            "write", "account_write",
            "system", "ROLE_SYSTEM",
            "server", "ROLE_SERVER",
            "user", "ROLE_USER"
    );

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object scopeClaim = jwt.getClaim("scope");

        return getScopes(scopeClaim).stream()
                .map(String::toLowerCase)
                .map(SCOPE_AUTHORITIES::get)
                .filter(Objects::nonNull)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> getScopes(Object scopeClaim) {
        if (scopeClaim instanceof Collection<?> scopes) {
            return scopes.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        if (scopeClaim instanceof String scope) {
            return Arrays.stream(scope.split("\\s+"))
                    .filter(scopeValue -> !scopeValue.isBlank())
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }
}
