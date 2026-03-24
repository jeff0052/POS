package com.developer.pos.v2.order.application.command;

import com.developer.pos.v2.order.domain.source.OrderSource;

import java.util.List;

public record ReplaceActiveTableOrderItemsCommand(
        Long storeId,
        Long tableId,
        String tableCode,
        OrderSource orderSource,
        Long memberId,
        List<ReplaceActiveTableOrderItemInput> items
) {
    public record ReplaceActiveTableOrderItemInput(
            Long skuId,
            String skuCode,
            String skuName,
            int quantity,
            long unitPriceCents,
            String remark
    ) {
    }
}
