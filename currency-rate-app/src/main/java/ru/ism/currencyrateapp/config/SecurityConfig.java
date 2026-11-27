package ru.ism.currencyrateapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain rateSecurityFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**").permitAll();
                    auth.anyRequest().authenticated();
                });
        http.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
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
