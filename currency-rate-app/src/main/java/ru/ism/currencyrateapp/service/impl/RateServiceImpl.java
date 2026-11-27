package ru.ism.currencyrateapp.service.impl;


import org.springframework.stereotype.Service;
import ru.ism.currencyrateapp.service.RateService;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class RateServiceImpl implements RateService {

    float euroRate = 90.09f;
    float dollarRate = 81.00f;

    @Override
    public float getEuroRate() {
        Random random = new Random();
        int num = random.nextInt(1000) - 500;
        return euroRate + (float) num / 100;
    }

    @Override
    public float getDollarRate() {
        Random random = new Random();
        int num = random.nextInt(1000) - 500;
        return dollarRate + (float) num / 100;
    }
}
