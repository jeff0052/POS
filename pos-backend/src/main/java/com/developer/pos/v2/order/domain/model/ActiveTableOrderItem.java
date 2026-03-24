package com.developer.pos.v2.order.domain.model;

import com.developer.pos.v2.catalog.domain.model.SkuRef;

public record ActiveTableOrderItem(
        SkuRef sku,
        int quantity,
        long unitPriceCents,
        String remark
) {
    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }
}
