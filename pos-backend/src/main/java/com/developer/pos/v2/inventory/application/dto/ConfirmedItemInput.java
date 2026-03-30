package com.developer.pos.v2.inventory.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmedItemInput(
    @NotNull Long inventoryItemId,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive Long unitPriceCents,
    LocalDate expiryDate   // nullable
) {}
