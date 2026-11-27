package ru.ism.frontapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountClientTest {

    private StubHttpServer server;
    private AccountClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        client = new AccountClient(WebClient.builder().build(), server.baseUrl());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void findByLoginGetsUserWithAuthenticatedLoginAndCachesResponse() {
        server.respond("/api/users/login", 200, MediaType.APPLICATION_JSON_VALUE, userJson("alice"));

        StepVerifier.create(client.findByLogin().contextWrite(authContext("alice")))
                .expectNext(new AccountClient.UserResponse(7L, "Alice", "alice", LocalDate.of(1990, 1, 1)))
                .verifyComplete();
        StepVerifier.create(client.findByLogin().contextWrite(authContext("alice")))
                .expectNext(new AccountClient.UserResponse(7L, "Alice", "alice", LocalDate.of(1990, 1, 1)))
                .verifyComplete();

        assertEquals(1, server.requestCount());
        assertEquals("GET", server.lastRequest().method());
        assertEquals("/api/users/login", server.lastRequest().path());
    }

    @Test
    void findOtherUserNamesReturnsListAndCachesResponse() {
        server.respond("/api/users/names/others", 200, MediaType.APPLICATION_JSON_VALUE,
                "[{\"id\":8,\"name\":\"Bob\",\"login\":\"bob\",\"birthDate\":\"1991-02-02\"}]");

        List<AccountClient.UserResponse> expected = List.of(
                new AccountClient.UserResponse(8L, "Bob", "bob", LocalDate.of(1991, 2, 2)));
        StepVerifier.create(client.findOtherUserNames().contextWrite(authContext("alice")))
                .expectNextSequence(expected)
                .verifyComplete();
        StepVerifier.create(client.findOtherUserNames().contextWrite(authContext("alice")))
                .expectNextSequence(expected)
                .verifyComplete();

        assertEquals(1, server.requestCount());
        assertEquals("/api/users/names/others", server.lastRequest().path());
    }

    @Test
    void updateUserSendsRequestAndInvalidatesUserCache() {
        server.respond("/api/users/login", 200, MediaType.APPLICATION_JSON_VALUE, userJson("alice"));
        server.respond("/api/users", 200, MediaType.APPLICATION_JSON_VALUE,
                "{\"id\":7,\"name\":\"Alice Updated\",\"login\":\"alice\",\"birthDate\":\"1990-01-01\"}");
        AccountClient.UserUpdateRequest update = new AccountClient.UserUpdateRequest("Alice Updated", null, "secret");

        StepVerifier.create(client.findByLogin().contextWrite(authContext("alice")))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(client.updateUser(update))
                .expectNext(new AccountClient.UserResponse(7L, "Alice Updated", "alice", LocalDate.of(1990, 1, 1)))
                .verifyComplete();
        StepVerifier.create(client.findByLogin().contextWrite(authContext("alice")))
                .expectNext(new AccountClient.UserResponse(7L, "Alice", "alice", LocalDate.of(1990, 1, 1)))
                .verifyComplete();

        assertEquals(3, server.requestCount());
        assertEquals("GET", server.lastRequest().method());
        assertEquals("/api/users/login", requestAt(server, 2).path());
        assertEquals("PUT", requestAt(server, 1).method());
        assertTrue(requestAt(server, 1).body().contains("\"name\":\"Alice Updated\""));
    }

    @Test
    void createAccountPostsCurrencyAndInitialBalance() {
        server.respond("/api/users/accounts", 201, MediaType.APPLICATION_JSON_VALUE,
                "{\"id\":2,\"userLogin\":\"alice\",\"number\":\"ACC-USD\",\"balance\":25.00,\"currency\":\"USD\",\"createdAt\":\"2026-01-01T10:00:00\",\"updatedAt\":\"2026-01-01T10:00:00\"}");

        StepVerifier.create(client.createAccount(new AccountClient.AccountCreateRequest(Currency.USD, new BigDecimal("25.00"))))
                .expectNext(new AccountClient.AccountResponse(2L, "alice", "ACC-USD", new BigDecimal("25.00"), "USD",
                        LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 10, 0)))
                .verifyComplete();

        assertEquals("POST", server.lastRequest().method());
        assertEquals("/api/users/accounts", server.lastRequest().path());
        assertTrue(server.lastRequest().body().contains("\"currency\":\"USD\""));
        assertTrue(server.lastRequest().body().contains("\"initialBalance\":25.00"));
    }

    @Test
    void deleteAccountUsesCurrencyInPath() {
        server.respond("/api/users/accounts/EUR", 204, MediaType.APPLICATION_JSON_VALUE, "");

        StepVerifier.create(client.deleteAccount(Currency.EUR)).verifyComplete();

        assertEquals("DELETE", server.lastRequest().method());
        assertEquals("/api/users/accounts/EUR", server.lastRequest().path());
    }

    @Test
    void getUserAccountsReturnsDecodedAccounts() {
        server.respond("/api/users/accounts", 200, MediaType.APPLICATION_JSON_VALUE,
                "[{\"id\":2,\"userLogin\":\"alice\",\"number\":\"ACC-EUR\",\"balance\":15.5,\"currency\":\"EUR\",\"createdAt\":\"2026-01-01T10:00:00\",\"updatedAt\":\"2026-01-01T10:00:00\"}]");

        StepVerifier.create(client.getUserAccounts())
                .expectNextMatches(account -> account.id() == 2L && account.currency().equals("EUR")
                        && account.balance().compareTo(new BigDecimal("15.5")) == 0)
                .verifyComplete();

        assertEquals("GET", server.lastRequest().method());
        assertEquals("/api/users/accounts", server.lastRequest().path());
    }

    @Test
    void updateUserMapsClientErrorBody() {
        server.respond("/api/users", 400, MediaType.TEXT_PLAIN_VALUE, "Invalid profile");

        StepVerifier.create(client.updateUser(new AccountClient.UserUpdateRequest("Alice", null, null)))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && error.getMessage().equals("Invalid profile"))
                .verify();
    }

    @Test
    void createAccountUsesDefaultMessageForEmptyClientError() {
        server.respond("/api/users/accounts", 400, MediaType.TEXT_PLAIN_VALUE, "");

        StepVerifier.create(client.createAccount(
                        new AccountClient.AccountCreateRequest(Currency.USD, BigDecimal.TEN)))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && error.getMessage().equals("Ошибка создания счета"))
                .verify();
    }

    @Test
    void deleteAccountMapsServerErrorBody() {
        server.respond("/api/users/accounts/EUR", 503, MediaType.TEXT_PLAIN_VALUE, "Account service unavailable");

        StepVerifier.create(client.deleteAccount(Currency.EUR))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Account service unavailable"))
                .verify();
    }

    private String userJson(String login) {
        return "{\"id\":7,\"name\":\"Alice\",\"login\":\"" + login + "\",\"birthDate\":\"1990-01-01\"}";
    }

    private reactor.util.context.Context authContext(String login) {
        return ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken(login, "token"));
    }

    private StubHttpServer.Request requestAt(StubHttpServer stub, int index) {
        return stub.requests().get(index);
    }
}
