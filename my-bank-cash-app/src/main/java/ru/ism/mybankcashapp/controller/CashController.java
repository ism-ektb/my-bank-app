package ru.ism.mybankcashapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ism.mybankcashapp.service.CashService;
import ru.ism.mybankdto.module.ActionDto;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Validated
public class CashController {

    Logger log = LoggerFactory.getLogger(CashController.class);
    private final CashService cashService;

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('account.write')")
        public Mono<Void> cash(@Valid @RequestBody ActionDto actionDto, JwtAuthenticationToken authentication) {
        log.info("cash requested, action: {}", actionDto);
        return cashService.cash(actionDto, authentication);
    }


}
