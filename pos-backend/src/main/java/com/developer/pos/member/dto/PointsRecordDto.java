package com.developer.pos.member.dto;

public record PointsRecordDto(
    Long id,
    String memberName,
    String changeType,
    Integer points,
    String source,
    String createdAt
) {
}
