package com.backendev.userservice.integration.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.entity.AuditLog;
import com.backendev.userservice.repository.AuditLogRepository;
import com.backendev.userservice.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuditServiceIT {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void testAuditLog_CreateAuditLogEntry() {
        AuditEventType eventType = AuditEventType.REGISTRATION;
        String email = "john@example.com";
        String description = "User registered";

        auditService.auditLog(eventType, email, description);

        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs)
                .isNotEmpty().hasSize(1);


        AuditLog savedLog = auditLogs.get(0);
        assertThat(savedLog.getAuditEventType()).isEqualTo(eventType);
        assertThat(savedLog.getEmail()).isEqualTo(email);
        assertThat(savedLog.getDescription()).isEqualTo(description);
    }

    @Test
    void testAuditLog_MultipleAuditEntries() {
        auditService.auditLog(AuditEventType.REGISTRATION, "john@example.com", "User registered");
        auditService.auditLog(AuditEventType.LOGIN_SUCCESS, "john@example.com", "Login successful");
        auditService.auditLog(AuditEventType.PROFILE_UPDATED, "john@example.com", "Profile updated");

        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(3);

        assertThat(auditLogs.get(0).getAuditEventType()).isEqualTo(AuditEventType.REGISTRATION);
        assertThat(auditLogs.get(1).getAuditEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(auditLogs.get(2).getAuditEventType()).isEqualTo(AuditEventType.PROFILE_UPDATED);
    }

    @Test
    void testAuditLog_LoginFailureEvent() {
        AuditEventType eventType = AuditEventType.LOGIN_FAILURE;
        String email = "";
        String description = "Invalid credentials";

        auditService.auditLog(eventType, email, description);

        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);

        AuditLog savedLog = auditLogs.get(0);
        assertThat(savedLog.getAuditEventType()).isEqualTo(AuditEventType.LOGIN_FAILURE);
        assertThat(savedLog.getDescription()).isEqualTo("Invalid credentials");
    }

    @Test
    void testAuditLog_AllFieldsMapped() {
        AuditEventType eventType = AuditEventType.PROFILE_UPDATED;
        String email = "test@example.com";
        String description = "User updated profile information";

        auditService.auditLog(eventType, email, description);

        var auditLogs = auditLogRepository.findAll();
        AuditLog savedLog = auditLogs.get(0);

        assertThat(savedLog.getAuditEventType()).isEqualTo(eventType);
        assertThat(savedLog.getEmail()).isEqualTo(email);
        assertThat(savedLog.getDescription()).isEqualTo(description);
        assertThat(savedLog.getTimestamp()).isNotNull();
        assertThat(savedLog.getId()).isNotNull();
    }

    @Test
    void testAuditLog_PersistenceAcrossTransactions() {
        auditService.auditLog(AuditEventType.REGISTRATION, "john@example.com", "User registered");

        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);

        assertThat(auditLogs.get(0).getId()).isNotNull();
    }
}
