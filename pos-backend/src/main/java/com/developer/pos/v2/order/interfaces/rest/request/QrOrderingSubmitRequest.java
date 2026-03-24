package com.developer.pos.v2.order.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record QrOrderingSubmitRequest(
        @NotBlank String storeCode,
        @NotBlank String tableCode,
        Long memberId,
        @NotEmpty List<@Valid ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long skuId,
            @NotBlank String skuCode,
            @NotBlank String skuName,
            @Positive int quantity,
            @Positive long unitPriceCents,
            String remark
    ) {
    }
}
