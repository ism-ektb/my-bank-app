package ru.ism.mybankcashapp.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ism.mybankcashapp.service.CashService;
import ru.ism.mybankdto.exception.ClientException;
import ru.ism.mybankdto.exception.ServerException;
import ru.ism.mybankdto.module.Action;
import ru.ism.mybankdto.module.ActionDto;
import ru.ism.mybankdto.module.CashMoney;
import ru.ism.mybankdto.module.Notification;

import java.util.Map;
import java.util.function.Function;

@Service
public class CashServiceImpl implements CashService {


    private final WebClient webClient;
    private final Map<Action, Function<CashMoney, Mono<Void>>> actionHandlers;
    private final String accountUrl;
    private final String notificationUrl;

    public CashServiceImpl(WebClient webClient,
                           @Value("${bank.accounts-service.base-url}") String accountUrl,
                           @Value("${bank.notification-url}") String notificationUrl) {
        this.webClient = webClient;
        this.accountUrl = accountUrl;
        this.notificationUrl = notificationUrl;
        this.actionHandlers = Map.of(Action.PUT, this::addCash, Action.GET, this::reduceCash);
    }

    @Override
    public Mono<Void> cash(ActionDto actionDto, JwtAuthenticationToken authenticationToken) {
        String login = authenticationToken.getToken().getClaimAsString("preferred_username");
        CashMoney cashMoney = new CashMoney(login, actionDto.sum());
        return actionHandlers.get(actionDto.action()).apply(cashMoney);
    }

    private Mono<Void> addCash(CashMoney cashMoney) {
        return webClient.put()
                .uri(accountUrl + "/account/debit")
                .bodyValue(cashMoney)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, responseEntity -> {
                    return Mono.error(new ClientException("CashService client Error. Status code: " + responseEntity.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, responseEntity -> {
                    return Mono.error(new ServerException("AccountService server Error. Status code: " + responseEntity.statusCode()));
                })
                .bodyToMono(Void.class)
                .then(webClient.post()
                        .uri(notificationUrl + "/notification")
                        .bodyValue(new Notification(String.format("Внесение денег на счет %s в сумме %d", cashMoney.login(), cashMoney.sum())))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(e -> Mono.empty())
                );
    }

    private Mono<Void> reduceCash(CashMoney cashMoney) {
        return webClient.put()
                .uri(accountUrl + "/account/credit")
                .bodyValue(cashMoney)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, responseEntity -> {
                    return Mono.error(new ClientException("CashService client Error. Status code: " + responseEntity.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, responseEntity -> {
                    return Mono.error(new ServerException("AccountService server Error. Status code: " + responseEntity.statusCode()));
                })
                .bodyToMono(Void.class)
                .then(webClient.post()
                        .uri(notificationUrl + "/notification")
                        .bodyValue(new Notification(String.format("Снятие денег на счет %s в сумме %d", cashMoney.login(), cashMoney.sum())))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .onErrorResume(e -> Mono.empty())
                );
    }

}
