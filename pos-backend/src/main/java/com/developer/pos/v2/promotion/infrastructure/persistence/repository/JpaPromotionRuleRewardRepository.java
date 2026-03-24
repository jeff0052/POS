package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaPromotionRuleRewardRepository extends JpaRepository<PromotionRuleRewardEntity, Long> {
    List<PromotionRuleRewardEntity> findByRuleIdIn(Collection<Long> ruleIds);
    Optional<PromotionRuleRewardEntity> findByRuleId(Long ruleId);
}
