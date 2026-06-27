package com.backendev.userservice.messaging;

import com.backendev.userservice.dto.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${user.topic}")
    private String userTopic;

    public void publishUserEvent(NotificationEvent notificationEvent) {
        try {
            String json = objectMapper.writeValueAsString(notificationEvent);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(userTopic, json);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("User event published successfully - Topic {} - Offset {}",
                            result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                }
                else {
                    log.error("Failed to publish user event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Exception in trying to send user events: {}", e.getMessage());
        }
    }
}
