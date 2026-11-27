package ru.ism.frontapp;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootTest
class FrontAppApplicationTests {

    private static final HttpServer AUTHORIZATION_SERVER = startMockAuthorizationServer();

    @DynamicPropertySource
    static void configureAuthorizationServer(DynamicPropertyRegistry registry) {
        String issuer = "http://127.0.0.1:" + AUTHORIZATION_SERVER.getAddress().getPort();
        registry.add("spring.security.oauth2.client.provider.my_front.issuer-uri", () -> issuer);
    }

    @Test
    void contextLoads() {
    }

    @AfterAll
    static void stopMockAuthorizationServer() {
        AUTHORIZATION_SERVER.stop(0);
    }

    private static HttpServer startMockAuthorizationServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/.well-known/openid-configuration", exchange -> {
                String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
                String response = """
                        {
                          "issuer": "%s",
                          "authorization_endpoint": "%s/oauth2/authorize",
                          "token_endpoint": "%s/oauth2/token",
                          "jwks_uri": "%s/oauth2/jwks",
                          "userinfo_endpoint": "%s/userinfo",
                          "response_types_supported": ["code"],
                          "subject_types_supported": ["public"],
                          "id_token_signing_alg_values_supported": ["RS256"]
                        }
                        """.formatted(issuer, issuer, issuer, issuer, issuer);
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

}
