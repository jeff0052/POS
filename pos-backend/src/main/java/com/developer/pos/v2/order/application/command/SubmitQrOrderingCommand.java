package com.developer.pos.v2.order.application.command;

import java.util.List;

public record SubmitQrOrderingCommand(
        String storeCode,
        String tableCode,
        Long memberId,
        List<SubmitQrOrderingItemInput> items
) {
    public record SubmitQrOrderingItemInput(
            Long skuId,
            String skuCode,
            String skuName,
            int quantity,
            long unitPriceCents,
            String remark
    ) {
    }
}
