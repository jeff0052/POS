package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateBindingRequest(
    @NotBlank String inclusionType, long surchargeCents,
    Integer maxQtyPerPerson, int sortOrder
) {}
