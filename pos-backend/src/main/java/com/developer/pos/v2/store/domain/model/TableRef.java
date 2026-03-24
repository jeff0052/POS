package com.developer.pos.v2.store.domain.model;

public record TableRef(
        Long storeId,
        Long tableId,
        String tableCode
) {
}
