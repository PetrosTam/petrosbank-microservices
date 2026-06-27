package com.backendev.userservice.mapper;

import com.backendev.userservice.dto.AuditLogDTO;
import com.backendev.userservice.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    AuditLog toEntity(AuditLogDTO dto);

}
