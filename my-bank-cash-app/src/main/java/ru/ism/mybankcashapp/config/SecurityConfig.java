package ru.ism.mybankcashapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .oauth2Client(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter authenticationConverter() {
        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new ReactiveJwtGrantedAuthoritiesConverterAdapter(this::extractAuthorities));
        return jwtConverter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> rawRoles)) {
            return Collections.emptyList();
        }
        var roles = rawRoles.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
        var authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        if (roles.contains("ACCOUNT_WRITE")) {
            authorities.add(new SimpleGrantedAuthority("account.write"));
        }
        return authorities;
    }
}
