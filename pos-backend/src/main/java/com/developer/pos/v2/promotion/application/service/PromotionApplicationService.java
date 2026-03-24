package com.developer.pos.v2.promotion.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleSummaryDto;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionHitEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleConditionEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleRewardEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleConditionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PromotionApplicationService implements UseCase {

    private final JpaPromotionRuleRepository promotionRuleRepository;
    private final JpaPromotionRuleConditionRepository promotionRuleConditionRepository;
    private final JpaPromotionRuleRewardRepository promotionRuleRewardRepository;
    private final JpaPromotionHitRepository promotionHitRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;

    public PromotionApplicationService(
            JpaPromotionRuleRepository promotionRuleRepository,
            JpaPromotionRuleConditionRepository promotionRuleConditionRepository,
            JpaPromotionRuleRewardRepository promotionRuleRewardRepository,
            JpaPromotionHitRepository promotionHitRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.promotionRuleConditionRepository = promotionRuleConditionRepository;
        this.promotionRuleRewardRepository = promotionRuleRewardRepository;
        this.promotionHitRepository = promotionHitRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionRuleSummaryDto> listActiveRules(Long storeId) {
        List<PromotionRuleEntity> rules = promotionRuleRepository.findActiveRules(storeId, OffsetDateTime.now());
        Map<Long, PromotionRuleConditionEntity> conditions = promotionRuleConditionRepository.findByRuleIdIn(
                        rules.stream().map(PromotionRuleEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(PromotionRuleConditionEntity::getRuleId, Function.identity(), (left, right) -> left));
        Map<Long, PromotionRuleRewardEntity> rewards = promotionRuleRewardRepository.findByRuleIdIn(
                        rules.stream().map(PromotionRuleEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(PromotionRuleRewardEntity::getRuleId, Function.identity(), (left, right) -> left));

        return rules.stream()
                .map(rule -> new PromotionRuleSummaryDto(
                        rule.getId(),
                        rule.getRuleCode(),
                        rule.getRuleName(),
                        rule.getRuleType(),
                        conditions.containsKey(rule.getId()) && conditions.get(rule.getId()).getThresholdAmountCents() != null
                                ? conditions.get(rule.getId()).getThresholdAmountCents()
                                : 0,
                        rewards.containsKey(rule.getId()) && rewards.get(rule.getId()).getDiscountAmountCents() != null
                                ? rewards.get(rule.getId()).getDiscountAmountCents()
                                : 0,
                        rule.getPriority()
                ))
                .toList();
    }

    @Transactional
    public PromotionEvaluationDto applyBestPromotion(String activeOrderId) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        List<PromotionRuleEntity> rules = promotionRuleRepository.findActiveRules(activeOrder.getStoreId(), OffsetDateTime.now());
        List<Long> ruleIds = rules.stream().map(PromotionRuleEntity::getId).toList();
        Map<Long, PromotionRuleConditionEntity> conditions = promotionRuleConditionRepository.findByRuleIdIn(ruleIds)
                .stream()
                .collect(Collectors.toMap(PromotionRuleConditionEntity::getRuleId, Function.identity(), (left, right) -> left));
        Map<Long, PromotionRuleRewardEntity> rewards = promotionRuleRewardRepository.findByRuleIdIn(ruleIds)
                .stream()
                .collect(Collectors.toMap(PromotionRuleRewardEntity::getRuleId, Function.identity(), (left, right) -> left));

        long pricingBase = Math.max(0, activeOrder.getOriginalAmountCents() - activeOrder.getMemberDiscountCents());

        PromotionRuleEntity matchedRule = rules.stream()
                .filter(rule -> "FULL_REDUCTION".equalsIgnoreCase(rule.getRuleType()))
                .filter(rule -> conditions.containsKey(rule.getId()))
                .filter(rule -> rewards.containsKey(rule.getId()))
                .filter(rule -> "THRESHOLD_AMOUNT".equalsIgnoreCase(conditions.get(rule.getId()).getConditionType()))
                .filter(rule -> conditions.get(rule.getId()).getThresholdAmountCents() != null)
                .filter(rule -> rewards.get(rule.getId()).getDiscountAmountCents() != null)
                .filter(rule -> pricingBase >= conditions.get(rule.getId()).getThresholdAmountCents())
                .max(Comparator
                        .comparingLong((PromotionRuleEntity rule) -> rewards.get(rule.getId()).getDiscountAmountCents())
                        .thenComparingLong(rule -> conditions.get(rule.getId()).getThresholdAmountCents())
                        .thenComparingInt(PromotionRuleEntity::getPriority))
                .orElse(null);

        promotionHitRepository.deleteByActiveOrderDbId(activeOrder.getId());

        long promotionDiscount = 0;
        String matchedRuleCode = null;
        String matchedRuleName = null;

        if (matchedRule != null) {
            PromotionRuleRewardEntity reward = rewards.get(matchedRule.getId());
            promotionDiscount = reward.getDiscountAmountCents() == null ? 0 : reward.getDiscountAmountCents();
            matchedRuleCode = matchedRule.getRuleCode();
            matchedRuleName = matchedRule.getRuleName();

            PromotionHitEntity hit = new PromotionHitEntity();
            hit.setActiveOrderDbId(activeOrder.getId());
            hit.setRuleId(matchedRule.getId());
            hit.setRuleCode(matchedRuleCode);
            hit.setRuleName(matchedRuleName);
            hit.setDiscountAmountCents(promotionDiscount);
            hit.setGiftSnapshotJson(null);
            promotionHitRepository.save(hit);
        }

        activeOrder.setPromotionDiscountCents(promotionDiscount);
        activeOrder.setPayableAmountCents(Math.max(
                0,
                activeOrder.getOriginalAmountCents() - activeOrder.getMemberDiscountCents() - promotionDiscount
        ));
        activeTableOrderRepository.save(activeOrder);

        return new PromotionEvaluationDto(
                activeOrder.getActiveOrderId(),
                matchedRuleCode,
                matchedRuleName,
                activeOrder.getOriginalAmountCents(),
                activeOrder.getMemberDiscountCents(),
                promotionDiscount,
                activeOrder.getPayableAmountCents()
        );
    }
}
