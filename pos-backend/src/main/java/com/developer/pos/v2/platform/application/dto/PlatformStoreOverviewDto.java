package com.developer.pos.v2.platform.application.dto;

public record PlatformStoreOverviewDto(
        Long storeId,
        Long merchantId,
        String storeCode,
        String storeName,
        long tableCount,
        long availableTables,
        long occupiedTables,
        long reservedTables,
        long pendingSettlementTables
) {
}
