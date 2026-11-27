package ru.ism.authserverapp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.ism.authserverapp.web.RegisterForm;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class AccountClientTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 12);

    private MockRestServiceServer server;
    private AccountClient accountClient;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        passwordEncoder = new BCryptPasswordEncoder();
        accountClient = new AccountClient(builder.build(), passwordEncoder);
    }

    @Test
    void getUserForLoginMapsAccountResponseToUserDetails() {
        server.expect(requestTo("http://localhost/api/users/login/alice/auth"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 12,
                          "name": "Alice",
                          "login": "alice",
                          "birthDate": "1990-05-12",
                          "password": "$2a$10$encodedPassword"
                        }
                        """, MediaType.APPLICATION_JSON));

        UserDetails user = accountClient.getUserForLogin("alice");

        assertEquals("alice", user.getUsername());
        assertEquals("$2a$10$encodedPassword", user.getPassword());
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(user.isEnabled());
        server.verify();
    }

    @Test
    void getUserForLoginThrowsWhenResponseBodyIsEmpty() {
        server.expect(requestTo("http://localhost/api/users/login/missing/auth"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> accountClient.getUserForLogin("missing"));

        assertEquals("User not found", error.getMessage());
        server.verify();
    }

    @Test
    void registerSendsFormDataWithEncodedPassword() {
        RegisterForm form = registerForm();
        server.expect(requestTo("http://localhost/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.login").value("alice"))
                .andExpect(jsonPath("$.birthDate").value("1990-05-12"))
                .andExpect(jsonPath("$.password").value(
                        matchesPattern("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")))
                .andRespond(withSuccess("{\"id\":12,\"name\":\"Alice\",\"login\":\"alice\",\"birthDate\":\"1990-05-12\"}",
                        MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> accountClient.register(form));
        server.verify();
    }

    @Test
    void registerThrowsIllegalArgumentExceptionWithClientErrorBody() {
        server.expect(requestTo("http://localhost/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("Login already exists"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountClient.register(registerForm()));

        assertEquals("Login already exists", error.getMessage());
        server.verify();
    }

    @Test
    void registerUsesDefaultMessageForEmptyClientErrorBody() {
        server.expect(requestTo("http://localhost/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accountClient.register(registerForm()));

        assertEquals("Ошибка регистрации", error.getMessage());
        server.verify();
    }

    @Test
    void registerThrowsIllegalStateExceptionWithServerErrorBody() {
        server.expect(requestTo("http://localhost/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("Account service failure"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> accountClient.register(registerForm()));

        assertEquals("Account service failure", error.getMessage());
        server.verify();
    }

    @Test
    void registerUsesDefaultMessageForEmptyServerErrorBody() {
        server.expect(requestTo("http://localhost/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> accountClient.register(registerForm()));

        assertEquals("Сервис аккаунтов недоступен", error.getMessage());
        server.verify();
    }

    private RegisterForm registerForm() {
        RegisterForm form = new RegisterForm();
        form.setName("Alice");
        form.setLogin("alice");
        form.setBirthDate(BIRTH_DATE);
        form.setPassword("secret123");
        return form;
    }
}
