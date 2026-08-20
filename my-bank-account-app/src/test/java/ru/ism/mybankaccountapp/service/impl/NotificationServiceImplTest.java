package ru.ism.mybankaccountapp.service.impl;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.ism.mybankaccountapp.service.NotificationService;
import ru.ism.mybankdto.module.Notification;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(topics = {"account"})
class NotificationServiceImplTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationService notificationService;

    @Test
    void sendNotification() {

        try (var consumerForTest = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("my-group", "true", embeddedKafkaBroker),
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            consumerForTest.subscribe(List.of("account"));
            notificationService.sendNotification(new Notification("test")).block();
            var inputMessage = KafkaTestUtils.getSingleRecord(consumerForTest, "account", Duration.ofSeconds(5));
            assertThat(inputMessage.key()).isEqualTo("account_service");
            assertThat(inputMessage.value()).isEqualTo("test");
        }
    }
}