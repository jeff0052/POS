package com.developer.pos.v2.channel.application.dto;

import java.time.OffsetDateTime;

public record ChannelDto(
        Long id,
        Long merchantId,
        String channelCode,
        String channelName,
        String channelType,
        String contactName,
        String contactPhone,
        String contactEmail,
        String trackingParam,
        String trackingUrlPrefix,
        String channelStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
