package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaPromotionRuleConditionRepository extends JpaRepository<PromotionRuleConditionEntity, Long> {
    List<PromotionRuleConditionEntity> findByRuleIdIn(Collection<Long> ruleIds);
    Optional<PromotionRuleConditionEntity> findByRuleId(Long ruleId);
}
