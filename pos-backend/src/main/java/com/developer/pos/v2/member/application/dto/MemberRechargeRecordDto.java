package com.developer.pos.v2.member.application.dto;

import java.time.OffsetDateTime;

public record MemberRechargeRecordDto(
        Long id,
        String memberName,
        String memberPhone,
        long amountCents,
        long bonusAmountCents,
        String status,
        OffsetDateTime createdAt
) {
}
