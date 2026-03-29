package com.developer.pos.v2.inventory.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateInventoryItemRequest(
    @NotBlank String itemCode,
    @NotBlank String itemName,
    String category,
    @NotBlank String unit,
    BigDecimal safetyStock,
    Long defaultSupplierId
) {}
