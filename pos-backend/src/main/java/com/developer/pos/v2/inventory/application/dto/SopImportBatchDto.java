package com.developer.pos.v2.inventory.application.dto;

import java.time.LocalDateTime;

public record SopImportBatchDto(
    Long id,
    Long storeId,
    String fileName,
    int totalRows,
    int successRows,
    int errorRows,
    String batchStatus,
    String errorDetails,
    LocalDateTime createdAt
) {}
