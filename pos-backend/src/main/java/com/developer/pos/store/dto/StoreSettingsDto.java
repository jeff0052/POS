package com.developer.pos.store.dto;

import java.util.Map;

public record StoreSettingsDto(
    Long storeId,
    String receiptTitle,
    String receiptFooter,
    Map<String, Object> printerConfig,
    Map<String, Object> paymentConfig
) {
}
