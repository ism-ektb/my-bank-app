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

    @Autowired
    private TransferClient transferClient;

    @PostMapping("/logout1")
    public Mono<String> logout(OAuth2AuthenticationToken token) {
        String id = ((OidcUser) token.getPrincipal()).getIdToken().getTokenValue();
        return Mono.empty().thenReturn("redirect:http://localhost/auth/realms/bank-realm/protocol/openid-connect/logout?post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A8084%2Flogout&id_token_hint=" + id);
    }

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    public Mono<String> index() {
        return Mono.empty().thenReturn("redirect:/account");
    }

    @GetMapping("/account")
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

    @PostMapping("/account")
    public Mono<String> editAccount(ServerWebExchange exchange) {
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
    public Mono<String> transferMoney(ServerWebExchange exchange) {
        return exchange.getFormData()
                .filter(formData -> formData.getFirst("value") != null && formData.getFirst("login") != null)
                .switchIfEmpty(Mono.error(new RuntimeException("Empty data")))
                .flatMap(data -> transferClient.transfer(Long.parseLong(data.getFirst("value")), data.getFirst("login")))
                .thenReturn("redirect:/account?info=ok")
                .onErrorReturn("redirect:/account?error=error");
    }
}
