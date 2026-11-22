package ru.ism.authserverapp.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class ClientCredentialsConfig {

    @Configuration
    @EnableWebSecurity
    public class SecurityConfig {

        @Bean
        @Order(1)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
                throws Exception {

            http
                    .oauth2AuthorizationServer((authorizationServer) -> {
                        http.securityMatcher(authorizationServer.getEndpointsMatcher());
                        authorizationServer
                                .oidc(Customizer.withDefaults());    // Enable OpenID Connect 1.0
                    })
                    .authorizeHttpRequests((authorize) ->
                            authorize
                                    .anyRequest().authenticated()
                    ).csrf(AbstractHttpConfigurer::disable)
                    // Redirect to the login page when not authenticated from the
                    // authorization endpoint
                    .exceptionHandling((exceptions) -> exceptions
                            .defaultAuthenticationEntryPointFor(
                                    new LoginUrlAuthenticationEntryPoint("/auth/login"),
                                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                            )
                    );

            return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
                throws Exception {
            http
                    .authorizeHttpRequests((authorize) -> authorize
                            .requestMatchers("/auth/register", "/auth/login", "/login/**", "/css/**", "/js/**", "/test/**").permitAll()
                            .anyRequest().authenticated()
                    )
                    // Кастомная страница входа и обработка POST-запроса формы
            //        .csrf(AbstractHttpConfigurer::disable)
                    .formLogin(form -> form
                            .loginPage("/auth/login")
                            .loginProcessingUrl("/login")
                            .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                            .permitAll()
                    );

            return http.build();
        }


        @Bean
        public JWKSource<SecurityContext> jwkSource() {
            KeyPair keyPair = generateRsaKey();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
            JWKSet jwkSet = new JWKSet(rsaKey);
            return new ImmutableJWKSet<>(jwkSet);
        }

        private static KeyPair generateRsaKey() {
            KeyPair keyPair;
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                keyPair = keyPairGenerator.generateKeyPair();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            return keyPair;
        }

        @Bean
        public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
            return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        }

        @Bean
        public AuthorizationServerSettings authorizationServerSettings() {
            return AuthorizationServerSettings.builder().build();
        }

        @Bean
        public BCryptPasswordEncoder bCryptPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

}