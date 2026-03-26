package com.developer.pos.v2.member.application.dto;

public record MemberSummaryDto(
        Long memberId,
        String memberNo,
        String name,
        String phone,
        String tierCode,
        long pointsBalance,
        long cashBalanceCents,
        long lifetimeSpendCents,
        long lifetimeRechargeCents,
        String memberStatus
) {
}
