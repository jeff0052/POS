package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmedItemInput(
    Long inventoryItemId,
    BigDecimal quantity,
    String unit,
    Long unitPriceCents,
    LocalDate expiryDate   // nullable
) {}
