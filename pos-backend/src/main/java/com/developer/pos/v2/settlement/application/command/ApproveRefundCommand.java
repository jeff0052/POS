package com.developer.pos.v2.settlement.application.command;

public record ApproveRefundCommand(String refundNo, Long approvedBy, boolean approved) {}
