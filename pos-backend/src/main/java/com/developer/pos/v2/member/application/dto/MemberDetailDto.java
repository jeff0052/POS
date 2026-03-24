package com.developer.pos.v2.member.application.dto;

public record MemberDetailDto(
        Long memberId,
        String memberNo,
        String name,
        String phone,
        String tierCode,
        String memberStatus,
        long pointsBalance,
        long cashBalanceCents,
        long lifetimeSpendCents,
        long lifetimeRechargeCents
) {
}
