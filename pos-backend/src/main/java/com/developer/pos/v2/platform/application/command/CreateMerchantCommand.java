package com.developer.pos.v2.platform.application.command;

public record CreateMerchantCommand(String merchantName, String timezone, String currencyCode) {}
