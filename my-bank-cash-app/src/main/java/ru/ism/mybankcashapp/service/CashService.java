package ru.ism.mybankcashapp.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.module.ActionDto;

public interface CashService {

    Mono<Void> cash(ActionDto actionDto, JwtAuthenticationToken authenticationToken);
}
