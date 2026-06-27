package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import com.backendev.notificationservice.entity.ProcessedEvent;
import com.backendev.notificationservice.repository.ProcessedEventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void processNotification(NotificationEvent event) {

        if(event.getEventId() != null && processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event skipped! Event with id {} with event type {} already exists", event.getEventId(), event.getEventType());
            return;
        }

        log.info("===== NOTIFICATION =====");
        log.info("Event ID: {}", event.getEventId());
        log.info("Type    : {}", event.getEventType());
        log.info("User ID : {}", event.getUserId());
        log.info("Email   : {}", event.getEmail());
        log.info("Subject : {}", event.getSubject());
        log.info("Message : {}", event.getMessage());

        emailService.sendEmail(event);

        if(event.getEventId() != null){
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .processedAt(Instant.now())
                    .build());
        }
    }
}
