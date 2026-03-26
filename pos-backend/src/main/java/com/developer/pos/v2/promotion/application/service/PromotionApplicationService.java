package com.developer.pos.v2.promotion.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleDetailDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleSummaryDto;
import com.developer.pos.v2.promotion.application.dto.UpsertPromotionRuleDto;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionHitEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleConditionEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleRewardEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleConditionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRewardRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final JpaSkuRepository skuRepository;
    private final ObjectMapper objectMapper;

    public PromotionApplicationService(
            JpaPromotionRuleRepository promotionRuleRepository,
            JpaPromotionRuleConditionRepository promotionRuleConditionRepository,
            JpaPromotionRuleRewardRepository promotionRuleRewardRepository,
            JpaPromotionHitRepository promotionHitRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSkuRepository skuRepository,
            ObjectMapper objectMapper
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.promotionRuleConditionRepository = promotionRuleConditionRepository;
        this.promotionRuleRewardRepository = promotionRuleRewardRepository;
        this.promotionHitRepository = promotionHitRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.skuRepository = skuRepository;
        this.objectMapper = objectMapper;
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

    @Transactional(readOnly = true)
    public PromotionRuleDetailDto getRule(Long ruleId) {
        PromotionRuleEntity rule = promotionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion rule not found: " + ruleId));
        PromotionRuleConditionEntity condition = promotionRuleConditionRepository.findByRuleId(ruleId).orElse(null);
        PromotionRuleRewardEntity reward = promotionRuleRewardRepository.findByRuleId(ruleId).orElse(null);
        return toDetailDto(rule, condition, reward);
    }

    @Transactional
    public PromotionRuleDetailDto createRule(UpsertPromotionRuleDto command) {
        validateUpsertCommand(command, null);
        PromotionRuleEntity existing = promotionRuleRepository.findByRuleCode(command.ruleCode().trim()).orElse(null);
        if (existing != null) {
            throw new IllegalStateException("Promotion rule code already exists: " + command.ruleCode());
        }

        PromotionRuleEntity rule = new PromotionRuleEntity();
        applyRuleFields(rule, command, true);
        PromotionRuleEntity savedRule = promotionRuleRepository.save(rule);

        PromotionRuleConditionEntity condition = promotionRuleConditionRepository.save(buildConditionEntity(savedRule.getId(), command));
        PromotionRuleRewardEntity reward = promotionRuleRewardRepository.save(buildRewardEntity(savedRule.getId(), command));
        return toDetailDto(savedRule, condition, reward);
    }

    @Transactional
    public PromotionRuleDetailDto updateRule(Long ruleId, UpsertPromotionRuleDto command) {
        validateUpsertCommand(command, ruleId);
        PromotionRuleEntity rule = promotionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion rule not found: " + ruleId));

        applyRuleFields(rule, command, false);
        PromotionRuleEntity savedRule = promotionRuleRepository.save(rule);

        PromotionRuleConditionEntity condition = promotionRuleConditionRepository.findByRuleId(ruleId).orElseGet(PromotionRuleConditionEntity::new);
        condition.setRuleId(ruleId);
        condition.setConditionType(command.conditionType().trim().toUpperCase());
        condition.setThresholdAmountCents(command.thresholdAmountCents());
        PromotionRuleConditionEntity savedCondition = promotionRuleConditionRepository.save(condition);

        PromotionRuleRewardEntity reward = promotionRuleRewardRepository.findByRuleId(ruleId).orElseGet(PromotionRuleRewardEntity::new);
        reward.setRuleId(ruleId);
        reward.setRewardType(command.rewardType().trim().toUpperCase());
        reward.setDiscountAmountCents(command.discountAmountCents());
        reward.setGiftSkuId(command.giftSkuId());
        reward.setGiftQuantity(command.giftQuantity());
        PromotionRuleRewardEntity savedReward = promotionRuleRewardRepository.save(reward);

        return toDetailDto(savedRule, savedCondition, savedReward);
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
                .filter(rule -> conditions.containsKey(rule.getId()))
                .filter(rule -> rewards.containsKey(rule.getId()))
                .filter(rule -> "THRESHOLD_AMOUNT".equalsIgnoreCase(conditions.get(rule.getId()).getConditionType()))
                .filter(rule -> conditions.get(rule.getId()).getThresholdAmountCents() != null)
                .filter(rule -> pricingBase >= conditions.get(rule.getId()).getThresholdAmountCents())
                .filter(rule -> isSupportedReward(rewards.get(rule.getId())))
                .filter(rule -> rule.getMaxUsage() == null || rule.getUsageCount() < rule.getMaxUsage())
                .max(Comparator
                        .comparingInt((PromotionRuleEntity rule) -> rewardRank(rewards.get(rule.getId())))
                        .thenComparingLong(rule -> rewardValue(rewards.get(rule.getId())))
                        .thenComparingLong(rule -> conditions.get(rule.getId()).getThresholdAmountCents())
                        .thenComparingInt(PromotionRuleEntity::getPriority))
                .orElse(null);

        promotionHitRepository.deleteByActiveOrderDbId(activeOrder.getId());

        long promotionDiscount = 0;
        String matchedRuleCode = null;
        String matchedRuleName = null;
        List<PromotionEvaluationDto.GiftItemDto> giftItems = List.of();

        if (matchedRule != null) {
            PromotionRuleRewardEntity reward = rewards.get(matchedRule.getId());
            matchedRuleCode = matchedRule.getRuleCode();
            matchedRuleName = matchedRule.getRuleName();
            String giftSnapshotJson = null;

            if ("DISCOUNT_AMOUNT".equalsIgnoreCase(reward.getRewardType())) {
                promotionDiscount = reward.getDiscountAmountCents() == null ? 0 : reward.getDiscountAmountCents();
            } else if ("DISCOUNT_PERCENT".equalsIgnoreCase(reward.getRewardType())) {
                int percent = reward.getDiscountPercent() == null ? 0 : reward.getDiscountPercent();
                promotionDiscount = (pricingBase * percent) / 100;
            } else if ("GIFT_SKU".equalsIgnoreCase(reward.getRewardType()) && reward.getGiftSkuId() != null) {
                SkuEntity giftSku = skuRepository.findById(reward.getGiftSkuId())
                        .orElseThrow(() -> new IllegalStateException("Gift SKU not found: " + reward.getGiftSkuId()));
                int quantity = reward.getGiftQuantity() == null ? 1 : reward.getGiftQuantity();
                giftItems = List.of(new PromotionEvaluationDto.GiftItemDto(
                        giftSku.getId(),
                        giftSku.getSkuName(),
                        quantity
                ));
                giftSnapshotJson = writeGiftSnapshot(giftSku, quantity);
            }

            PromotionHitEntity hit = new PromotionHitEntity();
            hit.setActiveOrderDbId(activeOrder.getId());
            hit.setRuleId(matchedRule.getId());
            hit.setRuleCode(matchedRuleCode);
            hit.setRuleName(matchedRuleName);
            hit.setDiscountAmountCents(promotionDiscount);
            hit.setGiftSnapshotJson(giftSnapshotJson);
            promotionHitRepository.save(hit);
            promotionRuleRepository.incrementUsageCount(matchedRule.getId());
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
                activeOrder.getPayableAmountCents(),
                giftItems
        );
    }

    private boolean isSupportedReward(PromotionRuleRewardEntity reward) {
        if (reward == null || reward.getRewardType() == null) {
            return false;
        }
        return "DISCOUNT_AMOUNT".equalsIgnoreCase(reward.getRewardType())
                || "DISCOUNT_PERCENT".equalsIgnoreCase(reward.getRewardType())
                || "GIFT_SKU".equalsIgnoreCase(reward.getRewardType());
    }

    private int rewardRank(PromotionRuleRewardEntity reward) {
        if (reward == null || reward.getRewardType() == null) {
            return 0;
        }
        if ("DISCOUNT_AMOUNT".equalsIgnoreCase(reward.getRewardType())) return 3;
        if ("DISCOUNT_PERCENT".equalsIgnoreCase(reward.getRewardType())) return 2;
        return 1;
    }

    private long rewardValue(PromotionRuleRewardEntity reward) {
        if (reward == null || reward.getRewardType() == null) {
            return 0;
        }
        if ("DISCOUNT_AMOUNT".equalsIgnoreCase(reward.getRewardType())) {
            return reward.getDiscountAmountCents() == null ? 0 : reward.getDiscountAmountCents();
        }
        return reward.getGiftQuantity() == null ? 1 : reward.getGiftQuantity();
    }

    private String writeGiftSnapshot(SkuEntity giftSku, int quantity) {
        try {
            return objectMapper.writeValueAsString(new PromotionEvaluationDto.GiftItemDto(
                    giftSku.getId(),
                    giftSku.getSkuName(),
                    quantity
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize gift snapshot.", exception);
        }
    }

    private void validateUpsertCommand(UpsertPromotionRuleDto command, Long currentRuleId) {
        if (command.merchantId() == null) {
            throw new IllegalArgumentException("merchantId must not be null.");
        }
        if (command.storeId() == null) {
            throw new IllegalArgumentException("storeId must not be null.");
        }
        if (command.ruleCode() == null || command.ruleCode().isBlank()) {
            throw new IllegalArgumentException("ruleCode must not be blank.");
        }
        if (command.ruleName() == null || command.ruleName().isBlank()) {
            throw new IllegalArgumentException("ruleName must not be blank.");
        }
        if (command.ruleType() == null || command.ruleType().isBlank()) {
            throw new IllegalArgumentException("ruleType must not be blank.");
        }
        if (command.conditionType() == null || command.conditionType().isBlank()) {
            throw new IllegalArgumentException("conditionType must not be blank.");
        }
        if (command.rewardType() == null || command.rewardType().isBlank()) {
            throw new IllegalArgumentException("rewardType must not be blank.");
        }
        if (command.startsAt() != null && command.endsAt() != null && command.endsAt().isBefore(command.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt.");
        }

        String ruleType = command.ruleType().trim().toUpperCase();
        String rewardType = command.rewardType().trim().toUpperCase();
        if (!List.of("FULL_REDUCTION", "GIFT_SKU").contains(ruleType)) {
            throw new IllegalArgumentException("Unsupported ruleType: " + command.ruleType());
        }
        if (!List.of("DISCOUNT_AMOUNT", "DISCOUNT_PERCENT", "GIFT_SKU").contains(rewardType)) {
            throw new IllegalArgumentException("Unsupported rewardType: " + command.rewardType());
        }
        if ("FULL_REDUCTION".equals(ruleType) && (command.thresholdAmountCents() == null || command.thresholdAmountCents() <= 0)) {
            throw new IllegalArgumentException("FULL_REDUCTION requires positive thresholdAmountCents.");
        }
        if ("DISCOUNT_AMOUNT".equals(rewardType) && (command.discountAmountCents() == null || command.discountAmountCents() <= 0)) {
            throw new IllegalArgumentException("DISCOUNT_AMOUNT reward requires positive discountAmountCents.");
        }
        if ("GIFT_SKU".equals(rewardType) && (command.giftSkuId() == null || command.giftQuantity() == null || command.giftQuantity() <= 0)) {
            throw new IllegalArgumentException("GIFT_SKU reward requires giftSkuId and positive giftQuantity.");
        }

        PromotionRuleEntity ruleWithSameCode = promotionRuleRepository.findByRuleCode(command.ruleCode().trim()).orElse(null);
        if (ruleWithSameCode != null && (currentRuleId == null || !ruleWithSameCode.getId().equals(currentRuleId))) {
            throw new IllegalStateException("Promotion rule code already exists: " + command.ruleCode());
        }
    }

    private void applyRuleFields(PromotionRuleEntity rule, UpsertPromotionRuleDto command, boolean creating) {
        if (creating) {
            rule.setId(null);
        }
        rule.setMerchantId(command.merchantId());
        rule.setStoreId(command.storeId());
        rule.setRuleCode(command.ruleCode().trim());
        rule.setRuleName(command.ruleName().trim());
        rule.setRuleType(command.ruleType().trim().toUpperCase());
        rule.setRuleStatus(command.ruleStatus() == null || command.ruleStatus().isBlank() ? "ACTIVE" : command.ruleStatus().trim().toUpperCase());
        rule.setPriority(command.priority());
        rule.setStartsAt(command.startsAt());
        rule.setEndsAt(command.endsAt());
    }

    private PromotionRuleConditionEntity buildConditionEntity(Long ruleId, UpsertPromotionRuleDto command) {
        PromotionRuleConditionEntity condition = new PromotionRuleConditionEntity();
        condition.setRuleId(ruleId);
        condition.setConditionType(command.conditionType().trim().toUpperCase());
        condition.setThresholdAmountCents(command.thresholdAmountCents());
        return condition;
    }

    private PromotionRuleRewardEntity buildRewardEntity(Long ruleId, UpsertPromotionRuleDto command) {
        PromotionRuleRewardEntity reward = new PromotionRuleRewardEntity();
        reward.setRuleId(ruleId);
        reward.setRewardType(command.rewardType().trim().toUpperCase());
        reward.setDiscountAmountCents(command.discountAmountCents());
        reward.setGiftSkuId(command.giftSkuId());
        reward.setGiftQuantity(command.giftQuantity());
        return reward;
    }

    private PromotionRuleDetailDto toDetailDto(
            PromotionRuleEntity rule,
            PromotionRuleConditionEntity condition,
            PromotionRuleRewardEntity reward
    ) {
        return new PromotionRuleDetailDto(
                rule.getId(),
                rule.getMerchantId(),
                rule.getStoreId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getRuleType(),
                rule.getRuleStatus(),
                rule.getPriority(),
                rule.getStartsAt(),
                rule.getEndsAt(),
                condition == null ? null : condition.getConditionType(),
                condition == null ? null : condition.getThresholdAmountCents(),
                reward == null ? null : reward.getRewardType(),
                reward == null ? null : reward.getDiscountAmountCents(),
                reward == null ? null : reward.getGiftSkuId(),
                reward == null ? null : reward.getGiftQuantity()
        );
    }
}
