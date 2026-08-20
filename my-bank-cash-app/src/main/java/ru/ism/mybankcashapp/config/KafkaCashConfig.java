package ru.ism.mybankcashapp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaCashConfig {

    @Bean
    NewTopic account() {
        return TopicBuilder.name("cash")
                .partitions(1)
                .replicas(1)
                .compact()
                .build();
    }
}
