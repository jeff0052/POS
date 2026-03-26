package com.developer.pos.v2.store.application.dto;

public record ReservationDto(
        Long reservationId,
        String reservationNo,
        Long storeId,
        Long tableId,
        String guestName,
        String reservationTime,
        int partySize,
        String reservationStatus,
        String area
) {
}
