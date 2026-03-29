package com.developer.pos.v2.inventory.interfaces.rest.request;

import java.math.BigDecimal;

public record UpdateInventoryItemRequest(
    String itemName,
    String category,
    BigDecimal safetyStock,
    Long defaultSupplierId
) {}
