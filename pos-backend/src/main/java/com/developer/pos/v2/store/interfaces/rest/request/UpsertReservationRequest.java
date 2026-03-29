package com.developer.pos.v2.store.interfaces.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertReservationRequest(
        @NotBlank String guestName,
        String contactPhone,
        @NotBlank String reservationTime,
        @NotNull @Min(1) @Max(20) Integer partySize,
        @NotBlank String reservationStatus,
        @NotBlank String area,
        Long tableId
) {
}
