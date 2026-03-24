package com.developer.pos.v2.order.application.dto;

public record QrOrderingContextDto(
        Long storeId,
        String storeCode,
        String storeName,
        Long tableId,
        String tableCode,
        String tableName,
        String tableStatus,
        ActiveTableOrderDto currentActiveOrder
) {
}
