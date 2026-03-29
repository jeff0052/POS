package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;

public record OcrLineItem(
    String rawText,
    BigDecimal qty,
    String unit,
    Long unitPriceCents,
    Long lineTotalCents
) {}
