package ru.ism.mybankcashapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ism.mybankcashapp.service.CashService;
import ru.ism.mybankdto.module.ActionDto;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('account.write')")
        public Mono<Void> cash(@RequestBody ActionDto actionDto, JwtAuthenticationToken authentication) {
        return cashService.cash(actionDto, authentication);
    }


}
