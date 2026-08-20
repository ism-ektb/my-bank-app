package ru.ism.mybankcashapp.service.impl;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.ism.mybankdto.module.Notification;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(topics = {"cash"})
class NotificationCashServiceImplTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationCashServiceImpl notificationCashService;

    @Test
    void sendNotification() {
        try (var consumerForTest = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("my-group", "true", embeddedKafkaBroker),
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            consumerForTest.subscribe(List.of("cash"));
            notificationCashService.sendNotification(new Notification("test")).block();
            var inputMessage = KafkaTestUtils.getSingleRecord(consumerForTest, "cash", Duration.ofSeconds(5));
            assertThat(inputMessage.key()).isEqualTo("cash_service");
            assertThat(inputMessage.value()).isEqualTo("test");
        }
    }
}