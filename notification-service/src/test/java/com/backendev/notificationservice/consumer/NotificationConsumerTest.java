package com.backendev.notificationservice.consumer;

import com.backendev.notificationservice.dto.NotificationEvent;
import com.backendev.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private String validJson;

    @BeforeEach
    void setUp() {
        validJson = """
                {
                  "eventId": "EVT-001",
                  "eventType": "USER_REGISTERED",
                  "userId": "USR-001",
                  "email": "jane@example.com",
                  "subject": "Welcome",
                  "message": "Account created"
                }
                """;
    }

    @Nested
    class NormalProcessing {

        @Test
        void shouldProcessUserEvent() {
            notificationConsumer.consumeUserEvent(validJson);
            verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
        }

        @Test
        void shouldProcessAccountEvent() {
            notificationConsumer.consumeAccountEvent(validJson);
            verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
        }

        @Test
        void shouldProcessTransactionEvent() {
            notificationConsumer.consumeTransactionEvent(validJson);
            verify(notificationService, times(1)).processNotification(any(NotificationEvent.class));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldDiscardMessage_whenJsonIsInvalid() {
            notificationConsumer.consumeUserEvent("{ not valid json }");
            verify(notificationService, never()).processNotification(any());
        }

        @Test
        void shouldPropagateException_whenServiceFails() {
            doThrow(new RuntimeException("SMTP unavailable"))
                    .when(notificationService).processNotification(any());

            assertThrows(RuntimeException.class,
                    () -> notificationConsumer.consumeUserEvent(validJson));
        }
    }
}