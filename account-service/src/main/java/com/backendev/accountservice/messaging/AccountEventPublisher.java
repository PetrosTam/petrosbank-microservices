package com.backendev.accountservice.messaging;

import com.backendev.accountservice.dto.NotificationEvent;
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
public class AccountEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${account.topic}")
    private String accountTopic;

    public void publishAccountEvent(NotificationEvent notificationEvent) {
        try {
            String json = objectMapper.writeValueAsString(notificationEvent);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(accountTopic, json);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Account event published successfully - Topic {} - Offset {}",
                            result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                }
                else {
                    log.error("Failed to publish account event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Exception in trying to send account events: {}", e.getMessage());
        }
    }
}
