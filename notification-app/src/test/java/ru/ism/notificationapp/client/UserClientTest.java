package ru.ism.notificationapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserClientTest {

    private MockRestServiceServer server;
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        userClient = new UserClient(builder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserSendsJwtAsBearerTokenAndMapsResponse() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("access-token")));
        server.expect(requestTo("http://localhost:8081/api/users/login"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 12,
                          "name": "Alice",
                          "login": "alice",
                          "birthDate": "1990-05-12",
                          "createdAt": "2026-01-01T10:15:30",
                          "updatedAt": "2026-01-02T11:20:45"
                        }
                        """, MediaType.APPLICATION_JSON));

        UserClient.UserResponse user = userClient.getUser();

        assertEquals(12L, user.id());
        assertEquals("Alice", user.name());
        assertEquals("alice", user.login());
        assertEquals(LocalDate.of(1990, 5, 12), user.birthDate());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 15, 30), user.createdAt());
        assertEquals(LocalDateTime.of(2026, 1, 2, 11, 20, 45), user.updatedAt());
        server.verify();
    }

    @Test
    void getUserDoesNotSetBearerTokenWithoutJwtAuthentication() {
        server.expect(requestTo("http://localhost:8081/api/users/login"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .andRespond(withSuccess("""
                        {
                          "id": 7,
                          "name": "Bob",
                          "login": "bob",
                          "birthDate": "1985-03-20",
                          "createdAt": "2026-01-01T00:00:00",
                          "updatedAt": "2026-01-01T00:00:00"
                        }
                        """, MediaType.APPLICATION_JSON));

        UserClient.UserResponse user = userClient.getUser();

        assertEquals("bob", user.login());
        server.verify();
    }

    private Jwt jwt(String tokenValue) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Jwt(tokenValue, now, now.plusSeconds(3600),
                Map.of("alg", "none"), Map.of("sub", "alice"));
    }
}
