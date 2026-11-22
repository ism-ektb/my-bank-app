package ru.ism.accountapp.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.ism.accountapp.dto.ApiDtos.UserCreateRequest;
import ru.ism.accountapp.dto.ApiDtos.UserResponse;
import ru.ism.accountapp.dto.ApiDtos.UserUpdateRequest;
import ru.ism.accountapp.dto.ApiDtos.UserAuthResponse;
import ru.ism.accountapp.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('SERVER')")
    public Mono<ResponseEntity<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @GetMapping("/users/login/{login}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<UserResponse>> getUserByLogin(@PathVariable String login) {
        return userService.getByLogin(login)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/users/{login}")
    @PreAuthorize("hasRole('USER') && hasAuthority('account_write')")
    public Mono<ResponseEntity<UserResponse>> updateUser(
            @PathVariable String login,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.update(login, request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/login/{login}/auth")
    @PreAuthorize("hasRole('SERVER')")
    public Mono<ResponseEntity<UserAuthResponse>> getUserByLoginForAuth(@PathVariable String login) {

        return userService.getByLoginForAuth(login)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .map(user -> ResponseEntity.ok(new UserAuthResponse(
                        user.id(),
                        user.name(),
                        user.login(),
                        user.birthDate(),
                        user.password()
                )));
    }
}