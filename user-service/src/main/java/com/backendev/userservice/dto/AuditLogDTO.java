package com.backendev.userservice.dto;

import com.backendev.userservice.audit.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private AuditEventType auditEventType;
    private String email;
    private String description;
}
