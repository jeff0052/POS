package com.developer.pos.v2.order.application.query;

public record GetActiveTableOrderQuery(
        Long storeId,
        Long tableId,
        String tableCode
) {
}
