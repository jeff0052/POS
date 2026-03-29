package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

public record StartBuffetRequest(@NotNull Long packageId, int guestCount, int childCount) {}
