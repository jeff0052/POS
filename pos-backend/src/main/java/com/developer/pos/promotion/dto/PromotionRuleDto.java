package com.developer.pos.promotion.dto;

public record PromotionRuleDto(
    Long id,
    String name,
    String type,
    String status,
    String ruleSummary,
    Integer priority
) {
}
