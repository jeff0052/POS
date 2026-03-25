package com.developer.pos.v2.order.application.dto;

public record MerchantAdminOrderItemDto(
        String productName,
        int quantity,
        long amountCents,
        long originalAmountCents,
        long memberBenefitCents,
        long promotionBenefitCents,
        boolean gift
) {
}
