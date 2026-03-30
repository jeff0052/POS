package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;

public record SopCsvRow(
    int rowNumber,
    Long skuId,
    String inventoryItemCode,
    BigDecimal consumptionQty,
    String consumptionUnit,
    BigDecimal baseMultiplier,
    String notes
) {}
