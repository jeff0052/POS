package com.developer.pos.v2.order.interfaces.rest.request;

import com.developer.pos.v2.order.domain.source.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ReplaceActiveTableOrderItemsRequest(
        @NotNull OrderSource orderSource,
        Long memberId,
        @NotEmpty List<@Valid ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long skuId,
            @NotBlank String skuCode,
            @NotBlank String skuName,
            @Positive int quantity,
            @Positive long unitPriceCents,
            String remark,
            String optionSnapshotJson
    ) {
    }
}
