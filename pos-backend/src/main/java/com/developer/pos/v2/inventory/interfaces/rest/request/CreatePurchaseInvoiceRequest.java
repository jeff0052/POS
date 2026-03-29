package com.developer.pos.v2.inventory.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePurchaseInvoiceRequest(
    @NotBlank String invoiceNo,
    Long supplierId,
    String supplierName,
    @NotNull LocalDate invoiceDate
) {}
