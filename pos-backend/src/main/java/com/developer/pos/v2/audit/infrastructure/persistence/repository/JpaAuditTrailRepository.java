package com.developer.pos.v2.audit.infrastructure.persistence.repository;

import com.developer.pos.v2.audit.infrastructure.persistence.entity.AuditTrailEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAuditTrailRepository extends JpaRepository<AuditTrailEntity, Long> {

    Optional<AuditTrailEntity> findByTrailNo(String trailNo);

    Page<AuditTrailEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Page<AuditTrailEntity> findByStoreIdAndTargetTypeOrderByCreatedAtDesc(Long storeId, String targetType, Pageable pageable);

    Page<AuditTrailEntity> findByStoreIdAndRequiresApprovalTrueAndApprovalStatusOrderByCreatedAtDesc(
            Long storeId, String approvalStatus, Pageable pageable);
}
