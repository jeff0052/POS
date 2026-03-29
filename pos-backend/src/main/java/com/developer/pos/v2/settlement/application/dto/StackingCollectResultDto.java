package com.developer.pos.v2.settlement.application.dto;

import java.util.List;

public record StackingCollectResultDto(
    Long settlementId,
    String settlementNo,
    List<Long> holdIds,
    String externalPaymentUrl,
    long externalPaymentCents
) {}
