package com.developer.pos.v2.store.application.dto;

public record TableCleanResultDto(
        String tableStatus,
        String newQrToken
) {
}
