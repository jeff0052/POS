package com.developer.pos.v2.kitchen.application.dto;

import java.time.LocalDateTime;

public record StationHeartbeatDto(
    Long stationId,
    String kdsHealthStatus,
    LocalDateTime lastHeartbeatAt
) {}
