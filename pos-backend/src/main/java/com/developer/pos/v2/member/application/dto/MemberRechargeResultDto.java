package com.developer.pos.v2.member.application.dto;

public record MemberRechargeResultDto(
        Long memberId,
        String rechargeNo,
        long amountCents,
        long bonusAmountCents,
        long cashBalanceCents
) {
}
