package ru.ism.frontapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ism.frontapp.client.*;
import ru.ism.frontapp.domain.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@Controller
public class MainController {

    private final AccountClient accountClient;
    private final RateClient rateClient;
    private final CashClient cashClient;
    private final ConvertClient convertClient;
    private final TransferClient transferClient;


    public MainController(AccountClient accountClient, RateClient rateClient, CashClient cashClient, ConvertClient convertClient, TransferClient transferClient) {
        this.accountClient = accountClient;
        this.rateClient = rateClient;
        this.cashClient = cashClient;
        this.convertClient = convertClient;
        this.transferClient = transferClient;
    }

    @GetMapping
    public Mono<String> index() {
        return Mono.just("redirect:/account");
    }

    @GetMapping("/account")
    public Mono<String> home(Model model, @RequestParam(required = false, name = "error", defaultValue = "") String error,
                             @RequestParam(required = false, value = "info", defaultValue = "") String info) {
        return accountClient.findOtherUserNames().collectList()
                .map(list -> model.addAttribute("accounts_all", list)).then(

                        accountClient.findByLogin()
                                .flatMap(user -> accountClient.getUserAccounts()
                                        .collectList()
                                        .map(accounts -> {
                                            model.addAttribute("accounts", accounts);
                                            return user;
                                        }))
                                .flatMap(user -> {
                                    model.addAttribute("name", user.name());
                                    model.addAttribute("birthdate", user.birthDate());
                                    if (error.equals("error")) {
                                        model.addAttribute("errors", "Ошибка в выполнении операции");
                                    }
                                    if (info.equals("ok")) {
                                        model.addAttribute("info", "Операция выполнена!");
                                    }

                                    if (rateClient == null) {
                                        model.addAttribute("euroRate", 0.0f);
                                        model.addAttribute("dollarRate", 0.0f);
                                        return Mono.just("main");
                                    }

                                    Mono<Float> euroRate = rateClient.getEuroRate().onErrorReturn(0.0f);
                                    Mono<Float> dollarRate = rateClient.getDollarRate().onErrorReturn(0.0f);
                                    return Mono.zip(euroRate, dollarRate)
                                            .map(rates -> {
                                                model.addAttribute("euroRate", rates.getT1());
                                                model.addAttribute("dollarRate", rates.getT2());
                                                return "main";
                                            });
                                }));
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

    @PostMapping("/cash")
    public Mono<String> cashOperation(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    Currency currency = Currency.valueOf(formData.getFirst("currency"));
                    BigDecimal amount = new BigDecimal(formData.getFirst("value"));

                    return switch (formData.getFirst("action")) {
                        case "PUT" -> cashClient.addBalance(currency, amount);
                        case "GET" -> cashClient.reduceBalance(currency, amount);
                        default -> Mono.error(new IllegalArgumentException("Неизвестный тип операции"));
                    };
                })
                .thenReturn("redirect:/account?info=ok")
                .doOnError(Throwable::printStackTrace)
                .onErrorReturn("redirect:/account?error=error");
    }

    @PostMapping("/convert")
    public Mono<String> convertCurrency(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    Currency sourceCurrency = Currency.valueOf(formData.getFirst("sourceCurrency"));
                    Currency targetCurrency = Currency.valueOf(formData.getFirst("targetCurrency"));
                    BigDecimal amount = new BigDecimal(formData.getFirst("amount"));

                    if (amount.signum() <= 0) {
                        return Mono.error(new IllegalArgumentException("Сумма конвертации должна быть положительной"));
                    }
                    if (sourceCurrency == targetCurrency) {
                        return Mono.error(new IllegalArgumentException("Исходная и целевая валюты должны отличаться"));
                    }
                    return convertClient.convertCurrency(new ConvertClient.FrontConvertRequest(targetCurrency, sourceCurrency, amount));

                })
                .thenReturn("redirect:/account?info=ok")
                .doOnError(Throwable::printStackTrace)
                .onErrorReturn("redirect:/account?error=error");
    }

    @PostMapping("/transfer")
    public Mono<String> transfer(ServerWebExchange exchange) {
        return exchange.getFormData()
               .filter(formData -> formData.getFirst("transfer_value") != null
                        && formData.getFirst("login") != null
                        && formData.getFirst("currency") != null)
                .switchIfEmpty(Mono.error(new RuntimeException("Empty data")))
                .flatMap(data -> transferClient.transferBalance(
                        new TransferClient.TransferRequest(data.getFirst("login"),
                                Currency.valueOf(data.getFirst("currency")),
                                new BigDecimal(data.getFirst("transfer_value"))))
                )
                .thenReturn("redirect:/account?info=ok")
                .onErrorReturn("redirect:/account?error=error");
    }

    private Mono<BigDecimal> getRate(Currency currency) {
        return switch (currency) {
            case RUB -> Mono.just(BigDecimal.ONE);
            case USD -> rateClient.getDollarRate().map(BigDecimal::valueOf);
            case EUR -> rateClient.getEuroRate().map(BigDecimal::valueOf);
        };
    }
}
