package com.developer.pos.v2.settlement.application.dto;

import java.time.OffsetDateTime;

public record RefundRecordDto(
    Long id,
    String refundNo,
    Long settlementId,
    String settlementNo,
    Long merchantId,
    Long storeId,
    long refundAmountCents,
    String refundType,
    String refundReason,
    String refundStatus,
    String paymentMethod,
    Long operatedBy,
    Long approvedBy,
    OffsetDateTime createdAt
) {}
