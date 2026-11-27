package ru.ism.accountapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.ism.accountapp.dto.ApiDtos.UserAuthResponse;
import ru.ism.accountapp.dto.ApiDtos.UserCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.UserResponse;
import ru.ism.accountapp.dto.ApiDtos.UserUpdateRequest;
import ru.ism.accountapp.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController controller;
    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken("alice", "token");
    private final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final UserResponse user = new UserResponse(
            1L, "Alice", "alice", LocalDate.of(1990, 1, 1), timestamp, timestamp);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserController(userService);
    }

    @Test
    void createUserReturnsCreatedResponse() {
        UserCreateRequest request = new UserCreateRequest(
                "Alice", "alice", LocalDate.of(1990, 1, 1), "secret");
        when(userService.create(request)).thenReturn(Mono.just(user));

        StepVerifier.create(controller.createUser(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals(user, response.getBody());
                })
                .verifyComplete();

        verify(userService).create(request);
    }

    @Test
    void getNamesOfOtherUsersUsesAuthenticatedLogin() {
        UserResponse otherUser = new UserResponse(
                2L, "Bob", "bob", LocalDate.of(1991, 2, 2), timestamp, timestamp);
        when(userService.getUsersByLogin("alice")).thenReturn(Flux.just(otherUser));

        StepVerifier.create(controller.getNamesOfOtherUsers(authentication))
                .expectNext(otherUser)
                .verifyComplete();

        verify(userService).getUsersByLogin("alice");
    }

    @Test
    void getUserByLoginReturnsAuthenticatedUser() {
        when(userService.getByLogin("alice")).thenReturn(Mono.just(user));

        StepVerifier.create(controller.getUserByLogin(authentication))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(user, response.getBody());
                })
                .verifyComplete();

        verify(userService).getByLogin("alice");
    }

    @Test
    void updateUserUsesAuthenticatedLoginAndReturnsUpdatedUser() {
        UserUpdateRequest request = new UserUpdateRequest("Alice Updated", null, null);
        when(userService.update("alice", request)).thenReturn(Mono.just(user));

        StepVerifier.create(controller.updateUser(authentication, request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(user, response.getBody());
                })
                .verifyComplete();

        verify(userService).update("alice", request);
    }

    @Test
    void getUserByLoginForAuthReturnsAuthDetails() {
        UserAuthResponse authResponse =
                new UserAuthResponse(1L, "Alice", "alice", LocalDate.of(1990, 1, 1), "encoded");
        when(userService.getByLoginForAuth("alice")).thenReturn(Mono.just(authResponse));

        StepVerifier.create(controller.getUserByLoginForAuth("alice"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(authResponse, response.getBody());
                })
                .verifyComplete();

        verify(userService).getByLoginForAuth("alice");
    }

    @Test
    void getUserByLoginForAuthReturnsNotFoundWhenServiceCompletesEmpty() {
        when(userService.getByLoginForAuth("missing")).thenReturn(Mono.empty());

        StepVerifier.create(controller.getUserByLoginForAuth("missing"))
                .expectErrorMatches(error -> error instanceof org.springframework.web.server.ResponseStatusException
                        && ((org.springframework.web.server.ResponseStatusException) error)
                        .getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }
}
