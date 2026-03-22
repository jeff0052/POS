package com.developer.pos.store.dto;

public record StoreDto(
    Long id,
    String name,
    String code,
    String address,
    String phone
) {
}
