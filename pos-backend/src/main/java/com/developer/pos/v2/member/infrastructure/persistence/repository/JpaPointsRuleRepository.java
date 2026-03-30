package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPointsRuleRepository extends JpaRepository<PointsRuleEntity, Long> {
    List<PointsRuleEntity> findByMerchantIdAndRuleStatus(Long merchantId, String ruleStatus);
}
