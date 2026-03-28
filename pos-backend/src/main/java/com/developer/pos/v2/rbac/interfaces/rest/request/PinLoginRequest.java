package com.developer.pos.v2.rbac.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PinLoginRequest {

    @NotNull(message = "storeId is required")
    private Long storeId;

    @NotBlank(message = "userCode is required")
    private String userCode;

    @NotBlank(message = "pin is required")
    private String pin;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
