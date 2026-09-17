package com.payhub.audit.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.domain.AuditLog;
import com.payhub.audit.repository.AuditLogRepository;

/**
 * Records and reads audit entries. {@link #record} carries no
 * transaction of its own — it joins the caller's transaction, so an audit row
 * is committed atomically with the business change it describes (and rolled
 * back with it if the change fails).
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorUserId, AuditAction action, String entityType, UUID entityId,
                       Map<String, Object> beforeValue, Map<String, Object> afterValue) {
        auditLogRepository.save(
                AuditLog.of(actorUserId, action, entityType, entityId, beforeValue, afterValue));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findForActor(UUID actorUserId) {
        return auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(actorUserId);
    }
}
