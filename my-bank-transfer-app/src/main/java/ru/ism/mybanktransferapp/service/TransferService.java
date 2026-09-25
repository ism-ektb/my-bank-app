package ru.ism.mybanktransferapp.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.TransferRequest;

/**
 * Определяет операции перевода средств между счетами.
 */
public interface TransferService {
    Mono<Void> transfer(TransferRequest transferRequest, JwtAuthenticationToken jwtAuthenticationToken);
}
