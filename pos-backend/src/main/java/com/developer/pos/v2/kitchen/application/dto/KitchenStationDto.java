package com.developer.pos.v2.kitchen.application.dto;

import java.time.LocalDateTime;

public record KitchenStationDto(
    Long id,
    Long storeId,
    String stationCode,
    String stationName,
    String stationType,
    String printerIp,
    String fallbackPrinterIp,
    String fallbackMode,
    String kdsHealthStatus,
    LocalDateTime lastHeartbeatAt,
    String stationStatus,
    int sortOrder
) {}
