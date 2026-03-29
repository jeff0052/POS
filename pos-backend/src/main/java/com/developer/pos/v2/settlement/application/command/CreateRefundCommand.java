package com.developer.pos.v2.settlement.application.command;

import java.util.List;

public record CreateRefundCommand(
        Long settlementId, long refundAmountCents, String refundType,
        String reason, Long operatedBy, long maxRefundCents,
        List<RefundItemCommand> refundItems
) {
    public record RefundItemCommand(Long itemId, int quantity) {}
}
