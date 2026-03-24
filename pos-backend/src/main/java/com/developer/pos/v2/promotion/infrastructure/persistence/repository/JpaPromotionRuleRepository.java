package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface JpaPromotionRuleRepository extends JpaRepository<PromotionRuleEntity, Long> {
    List<PromotionRuleEntity> findByStoreIdAndRuleStatusOrderByPriorityAscIdAsc(Long storeId, String ruleStatus);

    default List<PromotionRuleEntity> findActiveRules(Long storeId, OffsetDateTime now) {
        return findByStoreIdAndRuleStatusOrderByPriorityAscIdAsc(storeId, "ACTIVE").stream()
                .filter(rule -> rule.getStartsAt() == null || !rule.getStartsAt().isAfter(now))
                .filter(rule -> rule.getEndsAt() == null || !rule.getEndsAt().isBefore(now))
                .toList();
    }
}
