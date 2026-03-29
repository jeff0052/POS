package com.developer.pos.v2.kitchen.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KitchenTicketDto(
    Long ticketId,
    String ticketNo,
    Long storeId,
    Long tableId,
    String tableCode,
    Long stationId,
    int roundNumber,
    String ticketStatus,
    List<KitchenTicketItemDto> items,
    LocalDateTime submittedAt,
    LocalDateTime startedAt,
    LocalDateTime readyAt,
    LocalDateTime servedAt
) {}
