package com.developer.pos.order.dto;

public record QrOrderSettleRequest(
    String storeCode,
    String tableCode
) {
}
