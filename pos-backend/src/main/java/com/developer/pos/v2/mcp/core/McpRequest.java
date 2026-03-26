package com.developer.pos.v2.mcp.core;

import java.util.Map;

public record McpRequest(
    String tool,
    Map<String, Object> params,
    ActionContext context
) {}
