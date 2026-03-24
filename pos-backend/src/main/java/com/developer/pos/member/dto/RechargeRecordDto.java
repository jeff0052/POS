package com.developer.pos.member.dto;

public record RechargeRecordDto(
    Long id,
    String memberName,
    String memberPhone,
    Long amountCents,
    Long bonusAmountCents,
    String status,
    String createdAt
) {
}
