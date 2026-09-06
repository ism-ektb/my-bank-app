package ru.ism.mybanktransferapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.TransferRequest;
import ru.ism.mybanktransferapp.service.TransferService;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@Validated
public class TransferController {
    private final TransferService transferService;
    Logger log = LoggerFactory.getLogger(TransferController.class);

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('account.write')")
    public Mono<Void> transfer(@RequestBody @Valid TransferRequest transferRequest, JwtAuthenticationToken jwtAuthenticationToken) {
        log.info("transfer request: ");
        return transferService.transfer(transferRequest, jwtAuthenticationToken);
    }
}
