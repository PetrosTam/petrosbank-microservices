package com.backendev.notificationservice.consumer;

import com.backendev.notificationservice.dto.NotificationEvent;
import com.backendev.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserEvent(String message) {
        log.info("Received user event: {}", message);
        processEvent(message);
    }

    @KafkaListener(topics = "${notification.topics.account-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAccountEvent(String message) {
        log.info("Received account event: {}", message);
        processEvent(message);
    }

    @KafkaListener(topics = "${notification.topics.transaction-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTransactionEvent(String message) {
        log.info("Received transaction event: {}", message);
        processEvent(message);
    }

    private void processEvent(String message) {
        NotificationEvent event;
        try {
            event = objectMapper.readValue(message, NotificationEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON — discarding message. Error: {}", e.getMessage());
            return;
        }
        notificationService.processNotification(event);
    }
}
