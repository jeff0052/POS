package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaPointsBatchRepository extends JpaRepository<PointsBatchEntity, Long> {
    List<PointsBatchEntity> findByMemberIdAndBatchStatusOrderByExpiresAtAsc(Long memberId, String batchStatus);
    List<PointsBatchEntity> findByBatchStatusAndExpiresAtBefore(String batchStatus, LocalDateTime cutoff);
    boolean existsByMemberIdAndSourceTypeAndSourceRef(Long memberId, String sourceType, String sourceRef);
}
