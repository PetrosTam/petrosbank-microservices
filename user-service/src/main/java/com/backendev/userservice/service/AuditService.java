package com.backendev.userservice.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuditLogDTO;
import com.backendev.userservice.entity.AuditLog;
import com.backendev.userservice.mapper.AuditMapper;
import com.backendev.userservice.repository.AuditLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditMapper auditMapper;

    public void auditLog(AuditEventType auditEventType, String email, String description){
        AuditLogDTO auditLogDTO = new AuditLogDTO(auditEventType, email, description);
        AuditLog auditLog = auditMapper.toEntity(auditLogDTO);
        auditLogRepository.save(auditLog);
    }
}
