package ru.ism.mybanktransferapp.service.impl;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ru.ism.mybankdto.module.Notification;
import ru.ism.mybanktransferapp.service.NotificationTransferService;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты сервиса уведомлений transfer-сервиса.
 *
 * <p>Проверяют публикацию уведомлений в Kafka-топик переводов.</p>
 */
@SpringBootTest
@EmbeddedKafka(topics = {"transfer"})
class NotificationTransferServiceImplTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationTransferService notificationTransferService;

    /**
     * Проверяет отправку уведомления в Kafka-топик переводов.
     */
    @Test
    void sendNotification() {
        try (var consumerForTest = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("my-group", "true", embeddedKafkaBroker),
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            consumerForTest.subscribe(List.of("transfer"));
            notificationTransferService.sendNotification(new Notification("test")).block();
            var inputMessage = KafkaTestUtils.getSingleRecord(consumerForTest, "transfer", Duration.ofSeconds(5));
            assertThat(inputMessage.key()).isEqualTo("transfer_service");
            assertThat(inputMessage.value()).isEqualTo("test");
        }
    }
}