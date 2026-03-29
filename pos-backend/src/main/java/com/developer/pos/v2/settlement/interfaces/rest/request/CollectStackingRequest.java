package com.developer.pos.v2.settlement.interfaces.rest.request;

public record CollectStackingRequest(
    boolean usePoints,
    Long couponId,
    Integer couponLockVersion,
    boolean useCashBalance,
    String externalPaymentMethod
) {}
