package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record BuffetOrderValidationResult(List<ValidatedItem> items) {
    public record ValidatedItem(
        Long skuId, boolean buffetIncluded, long buffetSurchargeCents,
        Long buffetPackageId, boolean rejected, String rejectReason
    ) {}
}
