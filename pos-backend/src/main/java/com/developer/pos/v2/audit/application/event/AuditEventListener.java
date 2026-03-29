package com.developer.pos.v2.audit.application.event;

import com.developer.pos.v2.audit.infrastructure.persistence.entity.AuditTrailEntity;
import com.developer.pos.v2.audit.infrastructure.persistence.repository.JpaAuditTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async listener that persists audit trail records AFTER the business transaction commits.
 * Runs in a separate thread with its own transaction — never blocks or pollutes business logic.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final JpaAuditTrailRepository auditTrailRepository;

    public AuditEventListener(JpaAuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
        try {
            AuditTrailEntity trail = new AuditTrailEntity();
            trail.setTrailNo(event.trailNo());
            trail.setStoreId(event.storeId());
            trail.setActorType(event.actorType());
            trail.setActorId(event.actorId());
            trail.setActorName(event.actorName());
            trail.setAction(event.action());
            trail.setTargetType(event.targetType());
            trail.setTargetId(event.targetId());
            trail.setBeforeSnapshot(event.beforeSnapshot());
            trail.setAfterSnapshot(event.afterSnapshot());
            trail.setRiskLevel(event.riskLevel());
            trail.setRequiresApproval(event.requiresApproval());
            trail.setIpAddress(event.ipAddress());

            if (event.requiresApproval()) {
                trail.setApprovalStatus("PENDING");
            }

            auditTrailRepository.save(trail);
        } catch (Exception e) {
            log.warn("Failed to persist audit trail {}: {}", event.trailNo(), e.getMessage());
        }
    }
}
