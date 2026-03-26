package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpsertCatalogProductRequest(
        @NotBlank String storeCode,
        @NotNull Long categoryId,
        String productCode,
        @NotBlank String name,
        @NotBlank String status,
        @Valid @NotEmpty List<UpsertCatalogSkuItemRequest> skus,
        @Valid List<UpsertCatalogAttributeGroupRequest> attributeGroups,
        @Valid List<UpsertCatalogModifierGroupRequest> modifierGroups,
        @Valid List<UpsertCatalogComboSlotRequest> comboSlots
) {
    public record UpsertCatalogSkuItemRequest(
            Long skuId,
            String skuCode,
            @NotBlank String name,
            @Min(0) long priceCents,
            @NotBlank String status,
            @NotNull Boolean available
    ) {
    }

    public record UpsertCatalogAttributeGroupRequest(
            String code,
            @NotBlank String name,
            @NotBlank String selectionMode,
            @NotNull Boolean required,
            Integer minSelect,
            Integer maxSelect,
            @Valid List<UpsertCatalogAttributeValueRequest> values
    ) {
    }

    public record UpsertCatalogAttributeValueRequest(
            String code,
            @NotBlank String label,
            @Min(0) long priceDeltaCents,
            @NotNull Boolean defaultSelected,
            String kitchenLabel
    ) {
    }

    public record UpsertCatalogModifierGroupRequest(
            String code,
            @NotBlank String name,
            Integer freeQuantity,
            Integer minSelect,
            Integer maxSelect,
            @Valid List<UpsertCatalogModifierOptionRequest> options
    ) {
    }

    public record UpsertCatalogModifierOptionRequest(
            String code,
            @NotBlank String label,
            @Min(0) long priceDeltaCents,
            @NotNull Boolean defaultSelected,
            String kitchenLabel
    ) {
    }

    public record UpsertCatalogComboSlotRequest(
            String code,
            @NotBlank String name,
            Integer minSelect,
            Integer maxSelect,
            List<String> allowedSkuCodes
    ) {
    }
}
