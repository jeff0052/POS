package com.developer.pos.v2.gto.application.dto;

import java.time.LocalDate;

public record GenerateGtoBatchCommand(
        Long merchantId,
        Long storeId,
        LocalDate exportDate
) {
}
