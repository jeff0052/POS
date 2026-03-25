package com.developer.pos.v2.member.application.dto;

public record MemberPointsAdjustmentResultDto(
        Long memberId,
        String ledgerNo,
        long pointsDelta,
        long balanceAfter
) {
}
