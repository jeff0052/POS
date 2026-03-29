package com.developer.pos.v2.store.application.dto;

import java.time.OffsetDateTime;

public record QrTokenResultDto(
        String token,
        OffsetDateTime expiresAt,
        String qrUrl
) {
}
