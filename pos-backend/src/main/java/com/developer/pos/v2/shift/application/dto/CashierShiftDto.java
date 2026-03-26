package com.developer.pos.v2.shift.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CashierShiftDto(
    String shiftId, Long storeId, String cashierStaffId, String cashierName,
    String shiftStatus, OffsetDateTime openedAt, OffsetDateTime closedAt,
    long openingCashCents, Long closingCashCents, Long expectedCashCents,
    Long cashDifferenceCents, long totalSalesCents, long totalRefundsCents,
    int totalTransactionCount, String notes, List<SettlementItemDto> settlements
) {
    public record SettlementItemDto(String settlementNo, String paymentMethod, long amountCents, OffsetDateTime settledAt) {}
}
