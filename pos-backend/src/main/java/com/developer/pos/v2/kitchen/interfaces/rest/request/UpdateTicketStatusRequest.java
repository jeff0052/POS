package com.developer.pos.v2.kitchen.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTicketStatusRequest(
    @NotBlank String newStatus
) {}
