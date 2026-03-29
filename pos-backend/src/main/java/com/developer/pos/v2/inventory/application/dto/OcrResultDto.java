package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record OcrResultDto(
    String supplierName,
    String invoiceDate,
    Long totalAmountCents,
    BigDecimal avgConfidence,
    List<OcrMatchedItem> items
) {}
