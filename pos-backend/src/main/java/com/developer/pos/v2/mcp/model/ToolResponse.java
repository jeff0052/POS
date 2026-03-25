package com.developer.pos.v2.mcp.model;

public record ToolResponse<T>(
    boolean success,
    T data,
    String error,
    RiskLevel riskLevel,
    String message
) {
    public static <T> ToolResponse<T> ok(T data) {
        return new ToolResponse<>(true, data, null, null, null);
    }
    public static <T> ToolResponse<T> ok(T data, String message) {
        return new ToolResponse<>(true, data, null, null, message);
    }
    public static <T> ToolResponse<T> pendingApproval(T data, RiskLevel risk, String message) {
        return new ToolResponse<>(true, data, null, risk, message);
    }
    public static <T> ToolResponse<T> error(String error) {
        return new ToolResponse<>(false, null, error, null, null);
    }
}
