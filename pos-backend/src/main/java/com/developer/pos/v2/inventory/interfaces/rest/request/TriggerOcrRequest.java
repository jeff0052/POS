package com.developer.pos.v2.inventory.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record TriggerOcrRequest(@NotBlank String imageAssetId) {}
