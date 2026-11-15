package ru.ism.frontapp.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.client.AccountClient;
import ru.ism.frontapp.web.RegisterForm;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountClient accountClient;

    public AuthController(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @GetMapping("/register")
    public Mono<Rendering> registerPage() {
        return Mono.just(
                Rendering.view("register")
                        .modelAttribute("user", new RegisterForm())
                        .build()
        );
    }

    @PostMapping("/register")
    public Mono<Rendering> register(
            @Valid @ModelAttribute("user") RegisterForm form,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return Mono.just(
                    Rendering.view("register")
                            .modelAttribute("user", form)
                            .build()
            );
        }

        return accountClient.register(form)
                .thenReturn(
                        Rendering.redirectTo("/auth/login?success=" +
                                URLEncoder.encode("Регистрация успешна", StandardCharsets.UTF_8))
                                .build()
                )
                .onErrorResume(ex ->
                        Mono.just(
                                Rendering.redirectTo("/auth/register?error=" +
                                        URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8))
                                        .build()
                        )
                );
    }

    @GetMapping("/login")
    public Mono<Rendering> loginPage() {
        return Mono.just(
                Rendering.view("login")
                        .build()
        );
    }
}