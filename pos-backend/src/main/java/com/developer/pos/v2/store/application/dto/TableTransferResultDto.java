package com.developer.pos.v2.store.application.dto;

public record TableTransferResultDto(
        Long sourceTableId,
        Long destinationTableId,
        String sessionId,
        String tableStatus
) {
}
