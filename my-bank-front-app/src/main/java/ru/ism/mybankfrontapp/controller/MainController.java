package ru.ism.mybankfrontapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Action;
import ru.ism.mybankfrontapp.client.TransferClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Контроллер main.html.
 * <p>
 * Используемая модель для main.html:
 * model.addAttribute("name", name);
 * model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 * model.addAttribute("sum", sum);
 * model.addAttribute("accounts", accounts);
 * model.addAttribute("errors", errors);
 * model.addAttribute("info", info);
 * <p>
 * Поля модели:
 * name - Фамилия Имя текущего пользователя, String (обязательное)
 * birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 * sum - сумма на счету текущего пользователя, Integer (обязательное)
 * accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 * errors - список ошибок после выполнения действий, List<String> (не обязательное)
 * info - строка успешности после выполнения действия, String (не обязательное)
 * <p>
 * С примерами использования можно ознакомиться в тестовом классе заглушке AccountStub
 */
@Controller
public class MainController {
    // TODO: Удалить заглушку, так как используется только для ознакомительных целей
    @Autowired
    private TransferClient transferClient;

    @PostMapping("/logout1")
    public Mono<String> logout(ServerWebExchange exchange, OAuth2AuthenticationToken token) {
        String id = ((OidcUser) token.getPrincipal()).getIdToken().getTokenValue();

        exchange.getResponse().getCookies().remove("SESSION");
        return Mono.empty().thenReturn("redirect:http://localhost:8080/realms/bank-realm/protocol/openid-connect/logout?post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A8084%2Flogout&id_token_hint=" + id);
    }

    @GetMapping("/login?logout")
    public Mono<String> login() {
        return Mono.empty().thenReturn("redirect:http://localhost:8084");
    }

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    /* public String index() {
        return "redirect:/account";
    }

     */
    public Mono<String> index() {
        return Mono.empty().thenReturn("redirect:/account");
    }

    /**
     * GET /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для получения данных аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     */
    @GetMapping("/account")
    /*
    public String getAccount(Model model) {
       AccountResponseDto dto = transferClient.findAccountByLogin();
       System.out.println(dto);
       model.addAttribute("name", dto.name());
       model.addAttribute("sum", dto.balance());
       model.addAttribute("birthdate", dto.birthdate().format(DateTimeFormatter.ISO_DATE));
       model.addAttribute("account", dto.login());
       return "main";    }     */
    public Mono<String> account(Model model, @RequestParam(required = false, name = "error", defaultValue = "") String error,
                                @RequestParam(required = false, value = "info", defaultValue = "") String info) {
        return transferClient.findAllAccounts()
                .collectList()
                .map(list -> model.addAttribute("accounts", list)).then(
                        transferClient.findAccountByLogin()
                                .map(dto -> {
                                    model.addAttribute("name", dto.name());
                                    model.addAttribute("sum", dto.balance());
                                    model.addAttribute("birthdate", dto.birthdate().format(DateTimeFormatter.ISO_DATE));
                                    model.addAttribute("account", dto.login());
                                    if (error.equals("error")) {
                                        model.addAttribute("errors", "Ошибка в выполнении операции");
                                    }
                                    if (info.equals("ok")) {
                                        model.addAttribute("info", "Операция выполнена!");
                                    }
                                    return "main";
                                }));

    }

    /**
     * POST /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для изменения данных текущего пользователя по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     * <p>
     * Изменяемые данные:
     * 1. name - Фамилия Имя
     * 2. birthdate - дата рождения в формате YYYY-DD-MM
     */

    @PostMapping("/account")
  /*  public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        // TODO: Заменить на то, что описано в комментарии к методу
        AccountResponseDto dto = transferClient.updateAccount(name, birthdate);
        model.addAttribute("name", dto.name());
        model.addAttribute("sum", dto.balance());
        model.addAttribute("birthdate", dto.birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("account", dto.login());
        return "main";
    }

   */
    public Mono<String> editAccount(Model model, ServerWebExchange exchange
    ) {
        return exchange.getFormData()
                .flatMap(formData ->
                        transferClient.updateAccount(formData.getFirst("name"),
                                LocalDate.parse(formData.getFirst("birthdate"))))
                .thenReturn("redirect:/account?info=ok")
                .onErrorReturn("redirect:/account?error=error");


    }

    @PostMapping("/cash")
    public Mono<String> cashMoney(Model model, ServerWebExchange exchange) {
        return exchange.getFormData()
                .filter(formData -> formData.getFirst("value") != null)
                .switchIfEmpty(Mono.error(new RuntimeException("Empty data")))
                .flatMap(data -> transferClient.cashMoney(Long.parseLong(data.getFirst("value")), Action.valueOf(data.getFirst("action"))))
                .thenReturn("redirect:/account?info=ok")
                .onErrorReturn("redirect:/account?error=error");
    }

    @PostMapping("/transfer")
    public Mono<String> transferMoney(Model model, ServerWebExchange exchange) {

        return exchange.getFormData()
                .map(formData -> {
                    System.out.println(formData.getFirst("value") + "________" + formData.getFirst("login"));
                    return formData;
                })
                .filter(formData -> formData.getFirst("value") != null && formData.getFirst("login") != null)
                .switchIfEmpty(Mono.error(new RuntimeException("Empty data")))
                .flatMap(data -> transferClient.transfer(Long.parseLong(data.getFirst("value")), data.getFirst("login")))
                .thenReturn("redirect:/account?info=ok")
                .onErrorReturn("redirect:/account?error=error");
    }

/*
    /**
     * POST /cash.p
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     * <p>
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    /*
    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action
    ) {
        // TODO: Заменить на то, что описано в комментарии к методу
        accountStub.editCash(model, value, action);

        return "main";
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     * <p>
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */
    /*
    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login
    ) {
        // TODO: Заменить на то, что описано в комментарии к методу
        accountStub.transfer(model, value, login);

        return "main";
    }
    */
}
