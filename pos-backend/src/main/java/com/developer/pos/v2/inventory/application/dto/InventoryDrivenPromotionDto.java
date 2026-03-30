package com.developer.pos.v2.inventory.application.dto;

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
) {}
