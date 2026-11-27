package ru.ism.convertapp.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.ism.convertapp.client.AccountClient;
import ru.ism.convertapp.client.RateClient;
import ru.ism.convertapp.domain.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ConvertService {

    private final AccountClient accountClient;
    private final RateClient rateClient;

    public ConvertService(AccountClient accountClient, RateClient rateClient) {
        this.accountClient = accountClient;
        this.rateClient = rateClient;
    }

    public Mono<Void> convertCurrency(Currency toCurrency, Currency fromCurrency, BigDecimal sum) {
        if (toCurrency == null || fromCurrency == null) {
            return Mono.error(new IllegalArgumentException("Currencies must be provided"));
        }
        if (sum == null || sum.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }
        if (toCurrency == fromCurrency) {
            return Mono.error(new IllegalArgumentException("Source and target currencies must differ"));
        }

        return Mono.zip(getRate(fromCurrency), getRate(toCurrency))
                .flatMap(rates -> {
                    BigDecimal toSum = sum.multiply(rates.getT1())
                            .divide(rates.getT2(), 2, RoundingMode.HALF_UP);

                    return accountClient.convertBalance(new AccountClient.ConvertRequest(
                            toCurrency,
                            fromCurrency,
                            toSum,
                            sum
                    ));
                });
    }

    private Mono<BigDecimal> getRate(Currency currency) {
        return switch (currency) {
            case RUB -> Mono.just(BigDecimal.ONE);
            case USD -> rateClient.getDollarRate().map(BigDecimal::valueOf);
            case EUR -> rateClient.getEuroRate().map(BigDecimal::valueOf);
        };
    }

}
