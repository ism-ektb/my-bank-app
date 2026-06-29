package ru.ism.mybankgateway.config;

import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        return WebClient.builder()
                .filter(lbFunction)
                .build();
    }
}
