package ru.ism.accountapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.ism.accountapp.domain.User;
import ru.ism.accountapp.dto.ApiDtos.UserCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.UserResponse;
import ru.ism.accountapp.dto.ApiDtos.UserUpdateRequest;
import ru.ism.accountapp.dto.ApiDtos.UserAuthResponse;
import ru.ism.accountapp.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserResponse> create(UserCreateRequest request) {
        return userRepository.findByLogin(request.login())
                .flatMap(existing -> Mono.<User>error(
                        new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists")
                ))
                .switchIfEmpty(
                        userRepository.save(new User(
                                null,
                                request.name(),
                                request.login(),
                                request.birthDate(),
                                request.password(),
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        )))
                                .map(UserResponse::from);
    }

    public Mono<UserResponse> update(String login, UserUpdateRequest request) {
        return userRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(existing -> userRepository.save(new User(
                        existing.id(),
                        request.name() != null ? request.name() : existing.name(),
                        existing.login(),
                        request.birthDate() != null ? request.birthDate() : existing.birthDate(),
                        request.password() == null || request.password().isEmpty() ? existing.password() : new BCryptPasswordEncoder().encode(request.password()),
                        existing.createdAt(),
                        LocalDateTime.now()
                )).map(UserResponse::from));
    }

    public Mono<User> getById(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    public Mono<UserResponse> getByLogin(String login) {
        return userRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .map(UserResponse::from);
    }

    public Mono<UserAuthResponse> getByLoginForAuth(String login) {
        return userRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .map(UserAuthResponse::from);
    }

}
