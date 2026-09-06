package ru.ism.mybankaccountapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;
import ru.ism.mybankdto.exception.NoFoundException;

@RestControllerAdvice
public class ExceptionControllerHandler {

    Logger log = LoggerFactory.getLogger(ExceptionControllerHandler.class);

    @ExceptionHandler(NoFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ResponseEntity<Void>> handle(NoFoundException e) {
        log.error(e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ResponseEntity<Void>> handle(AuthorizationDeniedException e) {
        log.error(e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}
