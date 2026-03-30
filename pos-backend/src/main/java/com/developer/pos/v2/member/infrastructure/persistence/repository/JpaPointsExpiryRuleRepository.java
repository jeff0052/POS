package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsExpiryRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPointsExpiryRuleRepository extends JpaRepository<PointsExpiryRuleEntity, Long> {
    Optional<PointsExpiryRuleEntity> findByMerchantIdAndIsActiveTrue(Long merchantId);
}
