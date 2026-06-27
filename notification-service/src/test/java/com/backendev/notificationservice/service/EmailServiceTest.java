package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private NotificationEvent event;

    @BeforeEach
    void setUp() {
        event = NotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .userId("USR-001")
                .email("jane@example.com")
                .subject("Welcome to BankApp")
                .message("Your account has been created successfully.")
                .build();
    }

    @Test
    void sendEmail_shouldSendEmailSuccessfully() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(event);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldSendToCorrectRecipient() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage.getTo());
        assertEquals("jane@example.com", sentMessage.getTo()[0]);
    }

    @Test
    void sendEmail_shouldSetCorrectSubject() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertEquals("Welcome to BankApp", captor.getValue().getSubject());
    }

    @Test
    void sendEmail_shouldSetCorrectBody() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertEquals("Your account has been created successfully.", captor.getValue().getText());
    }

    @Test
    void sendEmail_shouldNotThrow_whenMailSenderFails() {
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendEmail(event));
    }

    @Test
    void sendEmail_shouldStillAttemptSend_whenMailFails() {
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(event);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

}