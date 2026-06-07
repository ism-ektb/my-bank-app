package ru.ism.mybanktransferapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.Transfer;
import ru.ism.mybankdto.module.TransferRequest;
import ru.ism.mybanktransferapp.service.TransferService;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public Mono<Void> transfer(@RequestBody TransferRequest transferRequest, JwtAuthenticationToken jwtAuthenticationToken) {
        return transferService.transfer(transferRequest, jwtAuthenticationToken);
    }
}
