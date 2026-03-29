package com.developer.pos.v2.inventory.application.dto;

import java.time.LocalDate;

public record PurchaseInvoiceDto(
    Long id,
    Long storeId,
    String invoiceNo,
    Long supplierId,
    String supplierName,
    LocalDate invoiceDate,
    Long totalAmountCents,
    String invoiceStatus,
    String ocrStatus,
    String scanImageUrl
) {}
