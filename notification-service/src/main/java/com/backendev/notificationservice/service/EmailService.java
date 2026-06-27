package com.backendev.notificationservice.service;

import com.backendev.notificationservice.dto.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(NotificationEvent notificationEvent) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(notificationEvent.getEmail());
            mailMessage.setSubject(notificationEvent.getSubject());
            mailMessage.setText(notificationEvent.getMessage());
            mailSender.send(mailMessage);

            log.info("Sent email to {} with subject {}", notificationEvent.getEmail(), notificationEvent.getSubject());
        } catch (MailException e) {
            log.error("Failed to send email to {} with error {}", notificationEvent.getEmail(), e.getMessage());
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
