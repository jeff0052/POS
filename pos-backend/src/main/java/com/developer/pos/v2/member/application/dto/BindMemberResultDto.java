package com.developer.pos.v2.member.application.dto;

public record BindMemberResultDto(
        Long memberId,
        String activeOrderId,
        String tierCode,
        long memberDiscountCents,
        long payableAmountCents
) {
}
