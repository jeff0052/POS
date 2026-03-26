package com.developer.pos.v2.mcp.core;

public record McpResponse(
    boolean success,
    String tool,
    Object result,
    String error
) {
    public static McpResponse ok(String tool, Object result) {
        return new McpResponse(true, tool, result, null);
    }

    public static McpResponse fail(String tool, String error) {
        return new McpResponse(false, tool, null, error);
    }
}
