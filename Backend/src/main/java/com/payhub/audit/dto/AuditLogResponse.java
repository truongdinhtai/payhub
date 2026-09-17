package com.payhub.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.domain.AuditLog;

/** API view of an audit entry. */
public record AuditLogResponse(
        UUID id,
        AuditAction action,
        String entityType,
        String entityId,
        Map<String, Object> before,
        Map<String, Object> after,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getCreatedAt());
    }
}
