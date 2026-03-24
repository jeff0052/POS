package com.developer.pos.member.dto;

public record MemberDto(
    Long id,
    String name,
    String phone,
    String tierName,
    Integer points,
    Long balanceCents,
    Long totalSpentCents,
    Long totalRechargeCents,
    String status
) {
}
