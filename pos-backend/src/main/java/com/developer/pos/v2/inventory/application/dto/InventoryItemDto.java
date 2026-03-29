package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;

public record InventoryItemDto(
    Long id,
    Long storeId,
    String itemCode,
    String itemName,
    String category,
    String unit,
    BigDecimal currentStock,
    BigDecimal safetyStock,
    String itemStatus,
    Long defaultSupplierId
) {}
