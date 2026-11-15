package ru.ism.authserverapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class ClientCredentialsConfig {

    // 1. Цепочка фильтров сервера авторизации (протокольные эндпоинты)
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer.oidc(withDefaults()) // Если требуется OIDC, можно включить; для чистого Client Credentials не обязательно
                )
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
                // Для Client Credentials обычно не требуется перенаправление на страницу входа, но для корректной обработки ошибок можно сохранить
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(org.springframework.http.MediaType.TEXT_HTML)
                        )
                )
                // Принимать JWT в качестве токена доступа (если в дальнейшем нужно обращаться к защищенным ресурсам)
                .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));

        return http.build();
    }

    // 2. Стандартная цепочка фильтров (для Client Credentials здесь можно оставить только базовую конфигурацию, форма входа не требуется)
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated());

        // Примечание: Client Credentials не требует formLogin, поэтому здесь не настраиваем форму входа
        return http.build();
    }

    // 3. Регистрация клиента (ключевой момент)
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("my-front")
                .clientSecret("{noop}my-secret") // {noop} означает отсутствие шифрования, только для тестирования
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                // *** Основной момент: разрешить только тип предоставления Client Credentials ***
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                // Настроить области действия в соответствии с бизнес-требованиями
                .scope("read")
                .scope("write")
                // Client Credentials не требует redirectUri, не устанавливайте его
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    // 4. Настройки сервера
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build(); // По умолчанию issuer — текущий адрес
    }
}