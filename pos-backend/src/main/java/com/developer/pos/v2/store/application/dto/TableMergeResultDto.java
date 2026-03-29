package com.developer.pos.v2.store.application.dto;

public record TableMergeResultDto(
        Long mergeRecordId,
        Long masterSessionId
) {
}
