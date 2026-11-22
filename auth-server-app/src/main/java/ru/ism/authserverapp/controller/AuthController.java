package ru.ism.authserverapp.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ism.authserverapp.client.AccountClient;
import ru.ism.authserverapp.web.RegisterForm;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountClient authClient;


    public AuthController(AccountClient authClient) {
        this.authClient = authClient;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("user") RegisterForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            // При ошибках возвращаемся на ту же страницу, форма уже в модели
            return "register";
        }
        try {
            authClient.register(form);
        } catch (RuntimeException e) {
            return "redirect:/auth/register?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);

        }

        // ... сохранение пользователя
        return "redirect:/auth/login?success=" + URLEncoder.encode("Регистрация успешна", StandardCharsets.UTF_8);
    }


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
