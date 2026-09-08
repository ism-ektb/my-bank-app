package ru.ism.mybankaccountapp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic account() {
        return TopicBuilder.name("account")
                .partitions(1)
                .replicas(1)
                .compact()
                .build();
    }
}
