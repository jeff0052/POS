package com.developer.pos.v2.member.application.dto;

import java.time.OffsetDateTime;

public record MemberPointsRecordDto(
        Long id,
        String memberName,
        String changeType,
        long points,
        String source,
        OffsetDateTime createdAt
) {
}
