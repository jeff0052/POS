package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BindSkuModifierRequest(
        @NotNull Long skuId,
        @NotNull List<ModifierGroupBinding> bindings
) {
    public record ModifierGroupBinding(
            @NotNull Long modifierGroupId,
            int sortOrder
    ) {
    }
}
