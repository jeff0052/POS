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

import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryPromotionApprovalService implements UseCase {

    private static final Set<String> VALID_DRAFT_STATUSES = Set.of("DRAFT", "APPROVED", "REJECTED", "EXPIRED");
    private static final String RULE_TYPE = "FULL_REDUCTION";
    private static final int RULE_PRIORITY = 50;
    private static final String CONDITION_TYPE = "THRESHOLD_AMOUNT";
    private static final String REWARD_TYPE = "DISCOUNT_PERCENT";

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
        rule.setRuleType(RULE_TYPE);
        rule.setRuleStatus("ACTIVE");
        rule.setPriority(RULE_PRIORITY);
        rule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC));
        rule.setEndsAt(draft.getExpiresAt().atOffset(ZoneOffset.UTC));
        PromotionRuleEntity savedRule = ruleRepository.save(rule);

        // Create PromotionRuleConditionEntity
        PromotionRuleConditionEntity condition = new PromotionRuleConditionEntity();
        condition.setRuleId(savedRule.getId());
        condition.setConditionType(CONDITION_TYPE);
        // Inventory-driven promotions apply regardless of order amount — the trigger is
        // the inventory condition (expiry/overstock), not a minimum spend threshold.
        condition.setThresholdAmountCents(0L);
        // NOTE: The promotion rule's SKU scope is defined by the condition's applicable_sku_ids.
        // The draft's suggestedSkuIds should be wired to the condition or reward entity.
        // PromotionRuleConditionEntity and PromotionRuleRewardEntity currently lack an applicableSkuIds field.
        conditionRepository.save(condition);

        // Create PromotionRuleRewardEntity
        PromotionRuleRewardEntity reward = new PromotionRuleRewardEntity();
        reward.setRuleId(savedRule.getId());
        reward.setRewardType(REWARD_TYPE);
        // NOTE: PromotionRuleRewardEntity.discountPercent is Integer.
        // Current auto-generated values (30/20/15/10%) are whole numbers.
        // Explicit rounding guards against future fractional values.
        reward.setDiscountPercent(draft.getSuggestedDiscountPercent()
            .setScale(0, RoundingMode.HALF_UP).intValueExact());
        rewardRepository.save(reward);

        draft.approve(userId, savedRule.getId());
        promotionRepository.save(draft);
        return InventoryDrivenPromotionDto.from(draft);
    }

    @Transactional
    public InventoryDrivenPromotionDto rejectDraft(Long storeId, Long draftId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PROMOTION_APPROVE");

        InventoryDrivenPromotionEntity draft = loadDraftForStore(draftId, storeId);
        if (!"DRAFT".equals(draft.getDraftStatus())) {
            throw new IllegalStateException("Promotion " + draftId + " is not in DRAFT status");
        }
        Long userId = AuthContext.current().userId();

        draft.reject(userId);
        promotionRepository.save(draft);
        return InventoryDrivenPromotionDto.from(draft);
    }

    @Transactional(readOnly = true)
    public List<InventoryDrivenPromotionDto> listDrafts(Long storeId, String status) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");

        if (status != null && !VALID_DRAFT_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Valid values: " + VALID_DRAFT_STATUSES);
        }

        List<InventoryDrivenPromotionEntity> entities;
        if (status != null && !status.isBlank()) {
            entities = promotionRepository.findByStoreIdAndDraftStatusOrderByCreatedAtDesc(storeId, status);
        } else {
            entities = promotionRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }
        return entities.stream().map(InventoryDrivenPromotionDto::from).collect(Collectors.toList());
    }

    private InventoryDrivenPromotionEntity loadDraftForStore(Long draftId, Long storeId) {
        InventoryDrivenPromotionEntity draft = promotionRepository.findById(draftId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion draft not found: " + draftId));
        if (!storeId.equals(draft.getStoreId())) {
            throw new SecurityException("Promotion draft " + draftId + " does not belong to store " + storeId);
        }
        return draft;
    }

}
