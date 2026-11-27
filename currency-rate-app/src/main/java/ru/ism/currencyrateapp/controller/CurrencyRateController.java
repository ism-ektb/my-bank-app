package ru.ism.currencyrateapp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ism.currencyrateapp.service.RateService;

@RestController
@RequestMapping("/rate")
public class CurrencyRateController {

    private final RateService rateService;

    public CurrencyRateController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/euro")
    @PreAuthorize("hasRole('USER')")
    public float getEuroRate() {
        return rateService.getEuroRate();
    }

    @GetMapping("/dollar")
    @PreAuthorize("hasRole('USER')")
    public float getDollarRate() {
        return rateService.getDollarRate();
    }
}
