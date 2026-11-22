package ru.ism.authserverapp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration client = ClientRegistration.withRegistrationId("auth-server-client")
                .clientId("auth-server-client")
                .clientSecret("654321")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .tokenUri("http://localhost:9000/oauth2/token")
                .scope("server")
                .build();

        return new InMemoryClientRegistrationRepository(client);
    }



    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider provider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository);

        manager.setAuthorizedClientProvider(provider);

        return manager;
    }

    @Bean
    public RestClient userServiceRestClient(OAuth2AuthorizedClientManager manager) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(manager);

        interceptor.setClientRegistrationIdResolver(request -> "auth-server-client");

        return RestClient.builder()
                .baseUrl("http://localhost:8081")
                .requestInterceptor(interceptor)
                .build();
    }
}
