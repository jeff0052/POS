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
        AuthenticatedActor actor = AuthContext.current();
        // SUPER_ADMIN / MERCHANT_OWNER can see all stores
        if (actor.hasPermission("STORE_MANAGE")) return;
        if (!actor.hasStoreAccess(storeId)) {
            throw new SecurityException("Access denied: no access to store " + storeId);
        }
    }

    /** Verify caller has access to a specific audit record's store */
    private void assertAuditRecordAccess(AuditTrailEntity trail) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.hasPermission("STORE_MANAGE")) return;
        if (!actor.hasStoreAccess(trail.getStoreId())) {
            throw new SecurityException("Access denied: audit record belongs to another store");
        }
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
