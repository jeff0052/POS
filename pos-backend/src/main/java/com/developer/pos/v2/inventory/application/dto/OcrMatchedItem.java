package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;

public record OcrMatchedItem(
    String rawText,
    Long matchedInventoryItemId,
    String matchedItemName,
    BigDecimal confidence,
    BigDecimal qty,
    String unit,
    Long unitPriceCents,
    Long lineTotalCents
) {}
