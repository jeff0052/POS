package com.developer.pos.v2.kitchen.application.dto;

public record KitchenTicketItemDto(
    Long skuId,
    String skuNameSnapshot,
    int quantity,
    String itemRemark,
    String optionSnapshotJson
) {}
