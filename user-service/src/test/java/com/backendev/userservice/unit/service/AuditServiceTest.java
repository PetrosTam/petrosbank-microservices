package com.backendev.userservice.unit.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuditLogDTO;
import com.backendev.userservice.entity.AuditLog;
import com.backendev.userservice.mapper.AuditMapper;
import com.backendev.userservice.repository.AuditLogRepository;
import com.backendev.userservice.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditService auditService;

    private AuditEventType testEventType;
    private String testEmail;
    private String testDescription;
    private AuditLog expectedEntity;

    @BeforeEach
    void setUp() {
        testEventType = AuditEventType.LOGIN_SUCCESS;
        testEmail = "test@example.com";
        testDescription = "User successful login";
        expectedEntity = new AuditLog();
    }

    @Test
    void auditLog_ShouldCreateDTOWithCorrectParameters() {
        when(auditMapper.toEntity(any(AuditLogDTO.class))).thenReturn(expectedEntity);

        auditService.auditLog(testEventType, testEmail, testDescription);

        ArgumentCaptor<AuditLogDTO> dtoCaptor = ArgumentCaptor.forClass(AuditLogDTO.class);
        verify(auditMapper).toEntity(dtoCaptor.capture());

        AuditLogDTO capturedDTO = dtoCaptor.getValue();
        assertEquals(testEventType, capturedDTO.getAuditEventType());
        assertEquals(testEmail, capturedDTO.getEmail());
        assertEquals(testDescription, capturedDTO.getDescription());
    }

    @Test
    void auditLog_ShouldCallMapperToConvertDTOToEntity() {
        when(auditMapper.toEntity(any(AuditLogDTO.class))).thenReturn(expectedEntity);

        auditService.auditLog(testEventType, testEmail, testDescription);

        verify(auditMapper, times(1)).toEntity(any(AuditLogDTO.class));
    }

    @Test
    void auditLog_ShouldSaveEntityToRepository() {
        when(auditMapper.toEntity(any(AuditLogDTO.class))).thenReturn(expectedEntity);

        auditService.auditLog(testEventType, testEmail, testDescription);

        verify(auditLogRepository, times(1)).save(expectedEntity);
    }

    @Test
    void auditLog_ShouldHandleDifferentEventTypes() {
        AuditEventType logoutEventType = AuditEventType.LOGIN_FAILURE;
        when(auditMapper.toEntity(any(AuditLogDTO.class))).thenReturn(expectedEntity);

        auditService.auditLog(logoutEventType, testEmail, testDescription);

        ArgumentCaptor<AuditLogDTO> dtoCaptor = ArgumentCaptor.forClass(AuditLogDTO.class);
        verify(auditMapper).toEntity(dtoCaptor.capture());

        AuditLogDTO capturedDTO = dtoCaptor.getValue();
        assertEquals(logoutEventType, capturedDTO.getAuditEventType());
    }

}