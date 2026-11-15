package ru.ism.frontapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class TestAuthController {

    @GetMapping("/test/auth")
    @ResponseBody
    public Mono<String> authStatus(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    String success = (String) session.getAttributes().get("authSuccessMessage");
                    String failure = (String) session.getAttributes().get("authFailureMessage");
                    String logout = (String) session.getAttributes().get("authLogoutMessage");

                    return Mono.just(
                            "success=" + success +
                            "; failure=" + failure +
                            "; logout=" + logout
                    );
                })
                .switchIfEmpty(Mono.just("success=null; failure=null; logout=null"));
    }

    @GetMapping("/test/secure")
    @ResponseBody
    public Mono<String> secureStatus(Authentication authentication) {
        return Mono.just("SECURE_OK; user=" + authentication.getName());
    }
}