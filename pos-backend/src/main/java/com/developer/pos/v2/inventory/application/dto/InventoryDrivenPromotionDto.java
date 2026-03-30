package com.developer.pos.v2.inventory.application.dto;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryDrivenPromotionDto(
    Long id,
    Long storeId,
    Long inventoryItemId,
    Long inventoryBatchId,
    String triggerType,
    BigDecimal suggestedDiscountPercent,
    String suggestedSkuIds,
    String draftStatus,
    Long promotionRuleId,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {
    public static InventoryDrivenPromotionDto from(InventoryDrivenPromotionEntity e) {
        return new InventoryDrivenPromotionDto(e.getId(), e.getStoreId(),
            e.getInventoryItemId(), e.getInventoryBatchId(), e.getTriggerType(),
            e.getSuggestedDiscountPercent(), e.getSuggestedSkuIds(),
            e.getDraftStatus(), e.getPromotionRuleId(), e.getExpiresAt(), e.getCreatedAt());
    }
}
