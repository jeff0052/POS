package com.developer.pos.v2.promotion.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleDetailDto;
import com.developer.pos.v2.promotion.application.dto.UpsertPromotionRuleDto;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionHitEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleConditionEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleRewardEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleConditionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRewardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionApplicationServiceTest {

    @Mock private JpaPromotionRuleRepository promotionRuleRepository;
    @Mock private JpaPromotionRuleConditionRepository promotionRuleConditionRepository;
    @Mock private JpaPromotionRuleRewardRepository promotionRuleRewardRepository;
    @Mock private JpaPromotionHitRepository promotionHitRepository;
    @Mock private JpaActiveTableOrderRepository activeTableOrderRepository;
    @Mock private JpaSkuRepository skuRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PromotionApplicationService service;

    @Nested
    @DisplayName("createRule validation")
    class CreateRuleValidation {

        @Test
        @DisplayName("rejects null merchantId")
        void rejectsNullMerchantId() {
            UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                    null, 1001L, "PROMO01", "Test", "FULL_REDUCTION", null, 1,
                    null, null, "THRESHOLD_AMOUNT", 5000L,
                    "DISCOUNT_AMOUNT", 1000L, null, null
            );

            assertThrows(IllegalArgumentException.class, () -> service.createRule(command));
        }

        @Test
        @DisplayName("rejects blank ruleCode")
        void rejectsBlankRuleCode() {
            UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                    1L, 1001L, "", "Test", "FULL_REDUCTION", null, 1,
                    null, null, "THRESHOLD_AMOUNT", 5000L,
                    "DISCOUNT_AMOUNT", 1000L, null, null
            );

            assertThrows(IllegalArgumentException.class, () -> service.createRule(command));
        }

        @Test
        @DisplayName("rejects FULL_REDUCTION with zero threshold")
        void rejectsZeroThreshold() {
            UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                    1L, 1001L, "PROMO01", "Test", "FULL_REDUCTION", null, 1,
                    null, null, "THRESHOLD_AMOUNT", 0L,
                    "DISCOUNT_AMOUNT", 1000L, null, null
            );

            assertThrows(IllegalArgumentException.class, () -> service.createRule(command));
        }

        @Test
        @DisplayName("rejects endsAt before startsAt")
        void rejectsInvalidDateRange() {
            OffsetDateTime now = OffsetDateTime.now();
            UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                    1L, 1001L, "PROMO01", "Test", "FULL_REDUCTION", null, 1,
                    now.plusDays(1), now, "THRESHOLD_AMOUNT", 5000L,
                    "DISCOUNT_AMOUNT", 1000L, null, null
            );

            assertThrows(IllegalArgumentException.class, () -> service.createRule(command));
        }

        @Test
        @DisplayName("rejects duplicate ruleCode")
        void rejectsDuplicateRuleCode() {
            when(promotionRuleRepository.findByRuleCode("PROMO01"))
                    .thenReturn(Optional.of(new PromotionRuleEntity()));

            UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                    1L, 1001L, "PROMO01", "Test", "FULL_REDUCTION", null, 1,
                    null, null, "THRESHOLD_AMOUNT", 5000L,
                    "DISCOUNT_AMOUNT", 1000L, null, null
            );

            assertThrows(IllegalStateException.class, () -> service.createRule(command));
        }
    }

    @Nested
    @DisplayName("applyBestPromotion")
    class ApplyBestPromotion {

        @Test
        @DisplayName("applies discount when order meets threshold")
        void appliesDiscount_whenThresholdMet() {
            ActiveTableOrderEntity order = buildOrder("ATO-001", 10000L, 0L);
            when(activeTableOrderRepository.findByActiveOrderId("ATO-001"))
                    .thenReturn(Optional.of(order));

            PromotionRuleEntity rule = buildRule(1L, "PROMO01", "Full Reduction", 1);
            when(promotionRuleRepository.findActiveRules(eq(1001L), any()))
                    .thenReturn(List.of(rule));

            PromotionRuleConditionEntity condition = buildCondition(1L, 5000L);
            when(promotionRuleConditionRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(condition));

            PromotionRuleRewardEntity reward = buildReward(1L, "DISCOUNT_AMOUNT", 1000L);
            when(promotionRuleRewardRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(reward));

            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PromotionEvaluationDto result = service.applyBestPromotion("ATO-001");

            assertEquals(1000L, result.promotionDiscountCents());
            assertEquals(9000L, result.payableAmountCents());
            assertEquals("PROMO01", result.matchedRuleCode());
        }

        @Test
        @DisplayName("no discount when order below threshold")
        void noDiscount_whenBelowThreshold() {
            ActiveTableOrderEntity order = buildOrder("ATO-002", 3000L, 0L);
            when(activeTableOrderRepository.findByActiveOrderId("ATO-002"))
                    .thenReturn(Optional.of(order));

            PromotionRuleEntity rule = buildRule(1L, "PROMO01", "Full Reduction", 1);
            when(promotionRuleRepository.findActiveRules(eq(1001L), any()))
                    .thenReturn(List.of(rule));

            PromotionRuleConditionEntity condition = buildCondition(1L, 5000L);
            when(promotionRuleConditionRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(condition));

            PromotionRuleRewardEntity reward = buildReward(1L, "DISCOUNT_AMOUNT", 1000L);
            when(promotionRuleRewardRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(reward));

            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PromotionEvaluationDto result = service.applyBestPromotion("ATO-002");

            assertEquals(0L, result.promotionDiscountCents());
            assertEquals(3000L, result.payableAmountCents());
            assertNull(result.matchedRuleCode());
        }

        @Test
        @DisplayName("selects higher discount when multiple rules match")
        void selectsHigherDiscount_whenMultipleMatch() {
            ActiveTableOrderEntity order = buildOrder("ATO-003", 15000L, 0L);
            when(activeTableOrderRepository.findByActiveOrderId("ATO-003"))
                    .thenReturn(Optional.of(order));

            PromotionRuleEntity rule1 = buildRule(1L, "PROMO01", "Small", 1);
            PromotionRuleEntity rule2 = buildRule(2L, "PROMO02", "Big", 2);
            when(promotionRuleRepository.findActiveRules(eq(1001L), any()))
                    .thenReturn(List.of(rule1, rule2));

            PromotionRuleConditionEntity cond1 = buildCondition(1L, 5000L);
            PromotionRuleConditionEntity cond2 = buildCondition(2L, 10000L);
            when(promotionRuleConditionRepository.findByRuleIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(cond1, cond2));

            PromotionRuleRewardEntity reward1 = buildReward(1L, "DISCOUNT_AMOUNT", 1000L);
            PromotionRuleRewardEntity reward2 = buildReward(2L, "DISCOUNT_AMOUNT", 3000L);
            when(promotionRuleRewardRepository.findByRuleIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(reward1, reward2));

            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PromotionEvaluationDto result = service.applyBestPromotion("ATO-003");

            assertEquals(3000L, result.promotionDiscountCents());
            assertEquals("PROMO02", result.matchedRuleCode());
        }

        @Test
        @DisplayName("member discount reduces pricing base for promotion threshold")
        void memberDiscountReducesPricingBase() {
            // Order 8000 cents, member discount 4000 cents -> pricing base = 4000
            // Promotion threshold 5000 -> should NOT match
            ActiveTableOrderEntity order = buildOrder("ATO-004", 8000L, 4000L);
            when(activeTableOrderRepository.findByActiveOrderId("ATO-004"))
                    .thenReturn(Optional.of(order));

            PromotionRuleEntity rule = buildRule(1L, "PROMO01", "Full Reduction", 1);
            when(promotionRuleRepository.findActiveRules(eq(1001L), any()))
                    .thenReturn(List.of(rule));

            PromotionRuleConditionEntity condition = buildCondition(1L, 5000L);
            when(promotionRuleConditionRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(condition));

            PromotionRuleRewardEntity reward = buildReward(1L, "DISCOUNT_AMOUNT", 1000L);
            when(promotionRuleRewardRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(reward));

            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PromotionEvaluationDto result = service.applyBestPromotion("ATO-004");

            assertEquals(0L, result.promotionDiscountCents());
            assertNull(result.matchedRuleCode());
        }

        @Test
        @DisplayName("payable never goes negative")
        void payableNeverNegative() {
            ActiveTableOrderEntity order = buildOrder("ATO-005", 500L, 0L);
            when(activeTableOrderRepository.findByActiveOrderId("ATO-005"))
                    .thenReturn(Optional.of(order));

            PromotionRuleEntity rule = buildRule(1L, "PROMO01", "Big Discount", 1);
            when(promotionRuleRepository.findActiveRules(eq(1001L), any()))
                    .thenReturn(List.of(rule));

            PromotionRuleConditionEntity condition = buildCondition(1L, 100L);
            when(promotionRuleConditionRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(condition));

            PromotionRuleRewardEntity reward = buildReward(1L, "DISCOUNT_AMOUNT", 9999L);
            when(promotionRuleRewardRepository.findByRuleIdIn(List.of(1L)))
                    .thenReturn(List.of(reward));

            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PromotionEvaluationDto result = service.applyBestPromotion("ATO-005");

            assertEquals(0L, result.payableAmountCents());
        }
    }

    // --- Helpers ---

    private ActiveTableOrderEntity buildOrder(String activeOrderId, long originalCents, long memberDiscountCents) {
        ActiveTableOrderEntity entity = new ActiveTableOrderEntity();
        entity.setId(1L);
        entity.setActiveOrderId(activeOrderId);
        entity.setStoreId(1001L);
        entity.setTableId(1L);
        entity.setOriginalAmountCents(originalCents);
        entity.setMemberDiscountCents(memberDiscountCents);
        entity.setPromotionDiscountCents(0L);
        entity.setPayableAmountCents(originalCents - memberDiscountCents);
        return entity;
    }

    private PromotionRuleEntity buildRule(Long id, String code, String name, int priority) {
        PromotionRuleEntity rule = new PromotionRuleEntity();
        rule.setId(id);
        rule.setMerchantId(1L);
        rule.setStoreId(1001L);
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setRuleType("FULL_REDUCTION");
        rule.setRuleStatus("ACTIVE");
        rule.setPriority(priority);
        return rule;
    }

    private PromotionRuleConditionEntity buildCondition(Long ruleId, long thresholdCents) {
        PromotionRuleConditionEntity condition = new PromotionRuleConditionEntity();
        condition.setRuleId(ruleId);
        condition.setConditionType("THRESHOLD_AMOUNT");
        condition.setThresholdAmountCents(thresholdCents);
        return condition;
    }

    private PromotionRuleRewardEntity buildReward(Long ruleId, String rewardType, long discountCents) {
        PromotionRuleRewardEntity reward = new PromotionRuleRewardEntity();
        reward.setRuleId(ruleId);
        reward.setRewardType(rewardType);
        reward.setDiscountAmountCents(discountCents);
        return reward;
    }
}
