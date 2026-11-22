package ru.ism.authserverapp.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.ism.authserverapp.web.RegisterForm;

import java.time.LocalDate;

@Component
public class AccountClient {
    private final RestClient restClient;
    private final BCryptPasswordEncoder passwordEncoder;

    public AccountClient(RestClient userServiceRestClient, BCryptPasswordEncoder passwordEncoder) {
        this.restClient = userServiceRestClient;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDetails getUserForLogin(String login) {


        UserDto userDto = restClient.get()
                .uri("/api/users/login/" + login + "/auth")
                .retrieve()
                .body(UserDto.class);


        if (userDto == null) {
            throw new RuntimeException("User not found");
        }
        return UserDto.from(userDto);

    }

    public void register(RegisterForm form) {
        restClient.post()
                .uri("/api/users")
                .body(new RegisterRequest(
                        form.getName(),
                        form.getLogin(),
                        form.getBirthDate(),
                        passwordEncoder.encode(form.getPassword())
                ))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    String msg = new String(response.getBody().readAllBytes());
                    if (msg.isBlank()) msg = "Ошибка регистрации";
                    throw new IllegalArgumentException(msg);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    String msg = new String(response.getBody().readAllBytes());
                    if (msg.isBlank()) msg = "Сервис аккаунтов недоступен";
                    throw new IllegalStateException(msg);
                })
                .body(UserResponse.class);
    }

    public record UserDto(Long id,
                          String name,
                          String login,
                          LocalDate birthDate,
                          String password) {
        public static UserDetails from(UserDto dto) {
            return User.withUsername(dto.login)
                    .password(dto.password())
                    .roles("USER").build();
        }
    }

    public record RegisterRequest(
            String name,
            String login,
            LocalDate birthDate,
            String password
    ) {
    }

    public record UserResponse(
            Long id,
            String name,
            String login,
            LocalDate birthDate
    ) {
    }
}