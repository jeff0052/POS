package com.developer.pos.gto.dto;

public record GtoBatchDto(
    Long id,
    String batchNo,
    String businessDate,
    String storeName,
    Integer tradeCount,
    Long netSalesCents,
    Long discountAmountCents,
    String syncStatus,
    String exportTime
) {
}
