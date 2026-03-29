package com.developer.pos.v2.store.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

public record TableMergeRequest(
        @NotNull Long masterTableId,
        @NotNull Long mergedTableId
) {
}
