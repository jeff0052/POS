package com.developer.pos.v2.channel.application.dto;

public record CommissionResultDto(
        Long commissionRecordId,
        String commissionNo,
        long commissionAmountCents
) {
}
