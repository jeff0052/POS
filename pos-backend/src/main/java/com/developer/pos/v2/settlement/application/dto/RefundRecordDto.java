package com.developer.pos.v2.settlement.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RefundRecordDto(
        Long id, String refundNo, Long settlementId, String settlementNo,
        Long merchantId, Long storeId, long refundAmountCents, String refundType,
        String refundReason, String refundStatus, String approvalStatus,
        String paymentMethod, Long operatedBy, Long approvedBy,
        long pointsReversedCents, long cashReversedCents, boolean couponReversed,
        String externalRefundStatus, List<RefundLineItemDto> lineItems,
        OffsetDateTime createdAt
) {
    public record RefundLineItemDto(Long id, Long orderItemId, int quantity, long refundAmountCents) {}
}
