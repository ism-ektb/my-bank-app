package ru.ism.accountapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.ism.accountapp.domain.User;
import ru.ism.accountapp.dto.ApiDtos.UserCreateRequest;
import ru.ism.accountapp.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void getUsersByLoginReturnsUsersExceptRequestedLogin() {
        when(userRepository.findAll()).thenReturn(Flux.just(
                user(1L, "alice", "Alice"),
                user(2L, "bob", "Bob")
        ));

        StepVerifier.create(userService.getUsersByLogin("alice"))
                .assertNext(response -> {
                    assertEquals("Bob", response.name());
                    assertEquals("bob", response.login());
                })
                .verifyComplete();
    }

    @Test
    void createSavesNewUserWhenLoginIsAvailable() {
        var request = new UserCreateRequest("Alice", "alice", LocalDate.of(1990, 1, 1), "secret");
        when(userRepository.findByLogin("alice")).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return Mono.just(new User(1L, saved.name(), saved.login(), saved.birthDate(), saved.password(),
                    saved.createdAt(), saved.updatedAt()));
        });

        StepVerifier.create(userService.create(request))
                .assertNext(response -> {
                    assertEquals(1L, response.id());
                    assertEquals("Alice", response.name());
                    assertEquals("alice", response.login());
                })
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }



    @Test
    void getByLoginReturnsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByLogin("missing")).thenReturn(Mono.empty());

        StepVerifier.create(userService.getByLogin("missing"))
                .expectErrorSatisfies(error -> {
                    var responseException = (ResponseStatusException) error;
                    assertEquals(HttpStatus.NOT_FOUND, responseException.getStatusCode());
                })
                .verify();
    }

    private static User user(Long id, String login, String name) {
        var now = LocalDateTime.now();
        return new User(id, name, login, LocalDate.of(1990, 1, 1), "password", now, now);
    }
}
