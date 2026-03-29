package com.developer.pos.v2.catalog.application.dto;

import java.time.OffsetDateTime;

public record BuffetStatusDto(
    Long sessionId, String buffetStatus,
    OffsetDateTime startedAt, OffsetDateTime endsAt,
    long remainingMinutes, long overtimeMinutes, long overtimeFeeCents,
    int guestCount, int childCount,
    String packageName, long packagePriceCents,
    boolean forceCheckout
) {}
