package com.developer.pos.v2.audit.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.audit.application.dto.AuditTrailDto;
import com.developer.pos.v2.audit.infrastructure.persistence.entity.AuditTrailEntity;
import com.developer.pos.v2.audit.infrastructure.persistence.repository.JpaAuditTrailRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuditTrailService implements UseCase {

    private final JpaAuditTrailRepository auditTrailRepository;

    public AuditTrailService(JpaAuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    /** Verify caller has access to the requested store */
    private void assertStoreAccess(Long storeId) {
        if (storeId == null) return; // merchant-level query, permission check is enough
        AuthenticatedActor actor = AuthContext.current();
        // SUPER_ADMIN (role=SUPER_ADMIN) bypasses store check — platform-wide access
        if ("SUPER_ADMIN".equals(actor.role())) return;
        // Everyone else must have explicit store access via user_store_access
        if (!actor.hasStoreAccess(storeId)) {
            throw new SecurityException("Access denied: no access to store " + storeId);
        }
    }

    /** Verify caller has access to a specific audit record's store */
    private void assertAuditRecordAccess(AuditTrailEntity trail) {
        if (trail.getStoreId() == null) {
            // Merchant-level record — only SUPER_ADMIN or same-merchant users can access
            // Permission gate (AUDIT_APPROVE) in SecurityConfig handles the rest
            return;
        }
        assertStoreAccess(trail.getStoreId());
    }

    @Transactional(readOnly = true)
    public Page<AuditTrailDto> listAuditLogs(Long storeId, String targetType, int page, int size) {
        assertStoreAccess(storeId);
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditTrailEntity> entities;

        if (targetType != null && !targetType.isBlank()) {
            entities = auditTrailRepository.findByStoreIdAndTargetTypeOrderByCreatedAtDesc(storeId, targetType, pageable);
        } else {
            entities = auditTrailRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }

        return entities.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditTrailDto> listPendingApprovals(Long storeId, int page, int size) {
        assertStoreAccess(storeId);
        Pageable pageable = PageRequest.of(page, size);
        return auditTrailRepository
                .findByStoreIdAndRequiresApprovalTrueAndApprovalStatusOrderByCreatedAtDesc(storeId, "PENDING", pageable)
                .map(this::toDto);
    }

    @Transactional
    public AuditTrailDto approve(Long auditId, String note) {
        AuditTrailEntity trail = auditTrailRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit trail not found: " + auditId));

        assertAuditRecordAccess(trail);

        if (!"PENDING".equals(trail.getApprovalStatus())) {
            throw new IllegalStateException("Audit trail is not pending approval: " + trail.getApprovalStatus());
        }

        AuthenticatedActor actor = AuthContext.current();
        trail.setApprovalStatus("APPROVED");
        trail.setApprovedBy(actor.userId());
        trail.setApprovedAt(OffsetDateTime.now());
        trail.setApprovalNote(note);
        auditTrailRepository.save(trail);

        return toDto(trail);
    }

    @Transactional
    public AuditTrailDto reject(Long auditId, String note) {
        AuditTrailEntity trail = auditTrailRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit trail not found: " + auditId));

        assertAuditRecordAccess(trail);

        if (!"PENDING".equals(trail.getApprovalStatus())) {
            throw new IllegalStateException("Audit trail is not pending approval: " + trail.getApprovalStatus());
        }

        AuthenticatedActor actor = AuthContext.current();
        trail.setApprovalStatus("REJECTED");
        trail.setApprovedBy(actor.userId());
        trail.setApprovedAt(OffsetDateTime.now());
        trail.setApprovalNote(note);
        auditTrailRepository.save(trail);

        return toDto(trail);
    }

    private AuditTrailDto toDto(AuditTrailEntity e) {
        return new AuditTrailDto(
                e.getId(),
                e.getTrailNo(),
                e.getStoreId(),
                e.getActorType(),
                e.getActorId(),
                e.getActorName(),
                e.getAction(),
                e.getTargetType(),
                e.getTargetId(),
                e.getRiskLevel(),
                e.getRequiresApproval(),
                e.getApprovalStatus(),
                e.getApprovedBy(),
                e.getApprovedAt(),
                e.getApprovalNote(),
                e.getCreatedAt()
        );
    }
}
