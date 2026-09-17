package com.payhub.audit.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.audit.dto.AuditLogResponse;
import com.payhub.audit.service.AuditService;
import com.payhub.security.CurrentUserProvider;

/** Exposes the current user's audit trail (their own actions only). */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit", description = "Compliance audit trail")
@SecurityRequirement(name = "bearer-jwt")
public class AuditLogController {

    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    public AuditLogController(AuditService auditService, CurrentUserProvider currentUserProvider) {
        this.auditService = auditService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List my audit entries (newest first)")
    public List<AuditLogResponse> myAuditTrail() {
        return auditService.findForActor(currentUserProvider.currentUserId()).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
