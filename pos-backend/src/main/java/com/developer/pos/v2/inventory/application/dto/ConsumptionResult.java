package com.developer.pos.v2.inventory.application.dto;

import java.math.BigDecimal;

public record ConsumptionResult(Long inventoryItemId, BigDecimal qty, String unit) {}
