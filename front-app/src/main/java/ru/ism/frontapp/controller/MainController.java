package ru.ism.frontapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.client.AccountClient;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class MainController {

    private final AccountClient accountClient;

    public MainController(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @GetMapping
    public Mono<String> index() {
        return Mono.just("redirect:/account");
    }

    @GetMapping("/account")
    public Mono<String> home(Model model, @RequestParam(required = false, name = "error", defaultValue = "") String error,
                             @RequestParam(required = false, value = "info", defaultValue = "") String info) {
        return accountClient.findByLogin()
                .flatMap(user -> accountClient.getUserAccounts()
                        .collectList()
                        .map(accounts -> {
                            model.addAttribute("accounts", accounts);
                            return user;
                        }))
                .map(user -> {
                    model.addAttribute("name", user.name());
                    model.addAttribute("birthdate", user.birthDate());
                    if (error.equals("error")) {
                        model.addAttribute("errors", "Ошибка в выполнении операции");
                    }
                    if (info.equals("ok")) {
                        model.addAttribute("info", "Операция выполнена!");
                    }
                    return "main";
                });
    }

    @PostMapping("/account")
    public Mono<String> editAccount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData ->
                        accountClient.updateUser(new AccountClient.UserUpdateRequest(
                                formData.getFirst("name"),
                                formData.getFirst("birthdate") != null ? LocalDate.parse(formData.getFirst("birthdate")) : null,
                                formData.getFirst("password")
                        )))
                .thenReturn("redirect:/account?info=ok")
                .doOnError(Throwable::printStackTrace)
                .onErrorReturn("redirect:/account?error=error");
    }

    @PostMapping("/account/create")
    public Mono<String> createAccount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData ->
                        accountClient.createAccount(new AccountClient.AccountCreateRequest(
                                Currency.valueOf(formData.getFirst("currency")),
                                new BigDecimal("0.00")
                        )))
                .thenReturn("redirect:/account?info=ok")
                .doOnError(Throwable::printStackTrace)
                .onErrorReturn("redirect:/account?error=error");
    }

    @PostMapping("/account/delete")
    public Mono<String> deleteAccount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData ->
                        accountClient.deleteAccount(Currency.valueOf(formData.getFirst("currency_")))
                )
                .thenReturn("redirect:/account?info=ok")
                .doOnError(Throwable::printStackTrace)
                .onErrorReturn("redirect:/account?error=error");
    }
}
