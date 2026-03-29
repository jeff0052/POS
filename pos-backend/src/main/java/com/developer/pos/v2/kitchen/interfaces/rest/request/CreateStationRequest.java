package com.developer.pos.v2.kitchen.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStationRequest(
    @NotBlank String stationCode,
    @NotBlank String stationName,
    String stationType,
    String printerIp,
    String fallbackPrinterIp,
    @NotNull Integer sortOrder
) {}
