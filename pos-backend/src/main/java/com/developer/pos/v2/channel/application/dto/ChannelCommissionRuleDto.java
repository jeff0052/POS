package com.developer.pos.v2.channel.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ChannelCommissionRuleDto(
        Long id,
        Long channelId,
        String ruleCode,
        String ruleName,
        String commissionType,
        BigDecimal commissionRatePercent,
        Long commissionFixedCents,
        String tierRulesJson,
        String calculationBase,
        String applicableStores,
        String applicableCategories,
        String applicableDiningModes,
        Long minCommissionCents,
        Long maxCommissionCents,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String ruleStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
