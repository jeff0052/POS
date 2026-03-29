package com.developer.pos.v2.store.application.dto;

public record ReservationDto(
        Long id, String reservationNo, Long storeId, Long tableId,
        String guestName, String contactPhone, String reservationTime,
        int partySize, String reservationStatus, String area
) {}
