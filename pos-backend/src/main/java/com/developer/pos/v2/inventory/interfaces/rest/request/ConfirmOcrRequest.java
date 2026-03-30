package com.developer.pos.v2.inventory.interfaces.rest.request;

import com.developer.pos.v2.inventory.application.dto.ConfirmedItemInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ConfirmOcrRequest(@NotEmpty @Valid List<ConfirmedItemInput> items) {}
