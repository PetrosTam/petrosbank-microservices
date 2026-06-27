package com.backendev.transactionservice.messaging;

import com.backendev.transactionservice.dto.NotificationEvent;
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
public class TransactionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${transaction.topic}")
    private String transactionTopic;

    public void publishTransactionEvent(NotificationEvent notificationEvent) {
        try {
            String json = objectMapper.writeValueAsString(notificationEvent);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(transactionTopic, json);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Transaction event published successfully - Topic {} - Offset {}",
                            result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                }
                else {
                    log.error("Failed to publish transaction event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Exception in trying to send transaction events: {}", e.getMessage());
        }
    }
}
