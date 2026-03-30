package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryDrivenPromotionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleConditionEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleRewardEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleConditionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryPromotionApprovalService implements UseCase {

    private final JpaInventoryDrivenPromotionRepository promotionRepository;
    private final JpaPromotionRuleRepository ruleRepository;
    private final JpaPromotionRuleConditionRepository conditionRepository;
    private final JpaPromotionRuleRewardRepository rewardRepository;
    private final StoreAccessEnforcer enforcer;

    public InventoryPromotionApprovalService(
            JpaInventoryDrivenPromotionRepository promotionRepository,
            JpaPromotionRuleRepository ruleRepository,
            JpaPromotionRuleConditionRepository conditionRepository,
            JpaPromotionRuleRewardRepository rewardRepository,
            StoreAccessEnforcer enforcer) {
        this.promotionRepository = promotionRepository;
        this.ruleRepository = ruleRepository;
        this.conditionRepository = conditionRepository;
        this.rewardRepository = rewardRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public InventoryDrivenPromotionDto approveDraft(Long storeId, Long draftId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PROMOTION_APPROVE");

        InventoryDrivenPromotionEntity draft = loadDraftForStore(draftId, storeId);
        if (!"DRAFT".equals(draft.getDraftStatus())) {
            throw new IllegalStateException("Promotion draft " + draftId + " is not in DRAFT status: " + draft.getDraftStatus());
        }
        Long userId = AuthContext.current().userId();
        Long merchantId = AuthContext.current().merchantId();

        // Create PromotionRuleEntity
        PromotionRuleEntity rule = new PromotionRuleEntity();
        rule.setMerchantId(merchantId);
        rule.setStoreId(storeId);
        rule.setRuleCode("INV-PROMO-" + draftId);
        rule.setRuleName(draft.getTriggerType() + " promotion (auto-generated)");
        rule.setRuleType("FULL_REDUCTION");
        rule.setRuleStatus("ACTIVE");
        rule.setPriority(50);
        rule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC));
        rule.setEndsAt(draft.getExpiresAt().atOffset(ZoneOffset.UTC));
        PromotionRuleEntity savedRule = ruleRepository.save(rule);

        // Create PromotionRuleConditionEntity
        PromotionRuleConditionEntity condition = new PromotionRuleConditionEntity();
        condition.setRuleId(savedRule.getId());
        condition.setConditionType("THRESHOLD_AMOUNT");
        condition.setThresholdAmountCents(0L);
        conditionRepository.save(condition);

        // Create PromotionRuleRewardEntity
        PromotionRuleRewardEntity reward = new PromotionRuleRewardEntity();
        reward.setRuleId(savedRule.getId());
        reward.setRewardType("DISCOUNT_PERCENT");
        reward.setDiscountPercent(draft.getSuggestedDiscountPercent().intValue());
        rewardRepository.save(reward);

        draft.approve(userId, savedRule.getId());
        promotionRepository.save(draft);
        return toDto(draft);
    }

    @Transactional
    public InventoryDrivenPromotionDto rejectDraft(Long storeId, Long draftId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PROMOTION_APPROVE");

        InventoryDrivenPromotionEntity draft = loadDraftForStore(draftId, storeId);
        Long userId = AuthContext.current().userId();

        draft.reject(userId);
        promotionRepository.save(draft);
        return toDto(draft);
    }

    @Transactional(readOnly = true)
    public List<InventoryDrivenPromotionDto> listDrafts(Long storeId, String status) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");

        List<InventoryDrivenPromotionEntity> entities;
        if (status != null && !status.isBlank()) {
            entities = promotionRepository.findByStoreIdAndDraftStatusOrderByCreatedAtDesc(storeId, status);
        } else {
            entities = promotionRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private InventoryDrivenPromotionEntity loadDraftForStore(Long draftId, Long storeId) {
        InventoryDrivenPromotionEntity draft = promotionRepository.findById(draftId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion draft not found: " + draftId));
        if (!storeId.equals(draft.getStoreId())) {
            throw new SecurityException("Promotion draft " + draftId + " does not belong to store " + storeId);
        }
        return draft;
    }

    InventoryDrivenPromotionDto toDto(InventoryDrivenPromotionEntity e) {
        return new InventoryDrivenPromotionDto(e.getId(), e.getStoreId(),
            e.getInventoryItemId(), e.getInventoryBatchId(), e.getTriggerType(),
            e.getSuggestedDiscountPercent(), e.getSuggestedSkuIds(),
            e.getDraftStatus(), e.getPromotionRuleId(), e.getExpiresAt(), e.getCreatedAt());
    }
}
