package com.developer.pos.v2.common.interfaces.rest;

public record NotImplementedPayload(
        String module,
        String capability,
        String nextStep
) {
}
