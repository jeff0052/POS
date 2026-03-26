package com.developer.pos.v2.settlement.application.command;

public record CreateRefundCommand(
    Long settlementId,
    long refundAmountCents,
    String refundType,
    String refundReason,
    Long operatedBy
) {}
