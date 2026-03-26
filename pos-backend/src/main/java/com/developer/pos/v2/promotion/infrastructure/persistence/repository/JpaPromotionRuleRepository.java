package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaPromotionRuleRepository extends JpaRepository<PromotionRuleEntity, Long> {

    @Modifying
    @Query("UPDATE V2PromotionRuleEntity r SET r.usageCount = r.usageCount + 1 WHERE r.id = :ruleId AND (r.maxUsage IS NULL OR r.usageCount < r.maxUsage)")
    int incrementUsageCount(@Param("ruleId") Long ruleId);
    List<PromotionRuleEntity> findByStoreIdAndRuleStatusOrderByPriorityAscIdAsc(Long storeId, String ruleStatus);
    Optional<PromotionRuleEntity> findByRuleCode(String ruleCode);

    default List<PromotionRuleEntity> findActiveRules(Long storeId, OffsetDateTime now) {
        return findByStoreIdAndRuleStatusOrderByPriorityAscIdAsc(storeId, "ACTIVE").stream()
                .filter(rule -> rule.getStartsAt() == null || !rule.getStartsAt().isAfter(now))
                .filter(rule -> rule.getEndsAt() == null || !rule.getEndsAt().isBefore(now))
                .toList();
    }
}
