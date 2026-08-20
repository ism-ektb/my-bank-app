package ru.ism.mybanknotificationapp.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = {"account", "transfer", "cash"}, groupId = "my_consumer")
    public void receiveMessage(ConsumerRecord<String, String> record) {
        log.info("<- {} [service = {}, key = {}, timestamp = {}]", record.value(), record.topic(), record.key(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()));


    }
}