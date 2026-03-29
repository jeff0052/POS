package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BindSkuRequest(
    @NotNull Long skuId, @NotBlank String inclusionType,
    long surchargeCents, Integer maxQtyPerPerson, int sortOrder
) {}
