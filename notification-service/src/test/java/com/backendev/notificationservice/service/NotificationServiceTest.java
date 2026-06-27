package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import com.backendev.notificationservice.entity.ProcessedEvent;
import com.backendev.notificationservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationEvent event;

    @BeforeEach
    void setUp() {
        event = NotificationEvent.builder()
                .eventId("EVT-001")
                .eventType("USER_REGISTERED")
                .userId("USR-001")
                .email("jane@example.com")
                .subject("Welcome to BankApp")
                .message("Your account has been created.")
                .build();
    }

    @Nested
    class NormalProcessing {

        @Test
        void shouldSendEmail_whenEventIsNew() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(false);

            notificationService.processNotification(event);

            verify(emailService, times(1)).sendEmail(event);
        }

        @Test
        void shouldSaveProcessedEvent_afterEmailSent() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(false);

            notificationService.processNotification(event);

            verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        }

        @Test
        void shouldSaveCorrectEventDetails() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(false);

            notificationService.processNotification(event);

            verify(processedEventRepository).save(argThat(saved ->
                    "EVT-001".equals(saved.getEventId()) &&
                            "USER_REGISTERED".equals(saved.getEventType()) &&
                            saved.getProcessedAt() != null
            ));
        }
    }

    @Nested
    class Idempotency {

        @Test
        void shouldSkipEmail_whenEventAlreadyProcessed() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(true);

            notificationService.processNotification(event);

            verify(emailService, never()).sendEmail(any());
        }

        @Test
        void shouldNotSave_whenEventIsDuplicate() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(true);

            notificationService.processNotification(event);

            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldProcess_whenEventIdIsNull() {
            // eventId null — no idempotency check, process normally
            NotificationEvent noIdEvent = NotificationEvent.builder()
                    .eventId(null)
                    .eventType("USER_REGISTERED")
                    .email("jane@example.com")
                    .subject("Welcome")
                    .message("Created")
                    .build();

            notificationService.processNotification(noIdEvent);

            verify(emailService, times(1)).sendEmail(noIdEvent);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldProcessTwoDifferentEvents_independently() {
            when(processedEventRepository.existsByEventId(any())).thenReturn(false);

            NotificationEvent event2 = NotificationEvent.builder()
                    .eventId("EVT-002")
                    .eventType("TRANSACTION_COMPLETED")
                    .email("jane@example.com")
                    .subject("Transaction Alert")
                    .message("Debited $500")
                    .build();

            notificationService.processNotification(event);
            notificationService.processNotification(event2);

            verify(emailService, times(2)).sendEmail(any());
            verify(processedEventRepository, times(2)).save(any());
        }
    }

    @Nested
    class RetryBehaviour {

        @Test
        void shouldNotSaveProcessedEvent_whenEmailFails() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(false);
            doThrow(new RuntimeException("SMTP unavailable"))
                    .when(emailService).sendEmail(any());

            assertThrows(RuntimeException.class,
                    () -> notificationService.processNotification(event));

            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldPropagateException_soKafkaCanRetry() {
            when(processedEventRepository.existsByEventId("EVT-001")).thenReturn(false);
            doThrow(new RuntimeException("SMTP unavailable"))
                    .when(emailService).sendEmail(any());

            assertThrows(RuntimeException.class,
                    () -> notificationService.processNotification(event));
        }
    }

}