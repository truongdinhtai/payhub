package com.payhub.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.domain.AuditLog;
import com.payhub.audit.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void record_persistsAuditLogWithGivenFields() {
        UUID actor = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Map<String, Object> before = Map.of("planCode", "FREE");
        Map<String, Object> after = Map.of("planCode", "PRO");

        auditService.record(actor, AuditAction.SUBSCRIPTION_PLAN_CHANGED, "Subscription",
                entityId, before, after);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorUserId()).isEqualTo(actor);
        assertThat(saved.getAction()).isEqualTo(AuditAction.SUBSCRIPTION_PLAN_CHANGED);
        assertThat(saved.getEntityType()).isEqualTo("Subscription");
        assertThat(saved.getEntityId()).isEqualTo(entityId.toString());
        assertThat(saved.getBeforeValue()).isEqualTo(before);
        assertThat(saved.getAfterValue()).isEqualTo(after);
    }

    @Test
    void findForActor_delegatesToRepository() {
        UUID actor = UUID.randomUUID();
        when(auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(actor))
                .thenReturn(List.of());

        assertThat(auditService.findForActor(actor)).isEmpty();
        verify(auditLogRepository).findByActorUserIdOrderByCreatedAtDesc(actor);
    }
}
