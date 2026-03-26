package com.developer.pos.v2.mcp.core;

import java.util.Map;

/**
 * Interface for all MCP tools. Each tool wraps a POS domain operation.
 */
public interface McpTool {

    /** Unique tool name, e.g. "catalog.list_products" */
    String name();

    /** Human-readable description for the AI agent */
    String description();

    /** JSON Schema describing the input parameters */
    Map<String, Object> inputSchema();

    /** Execute the tool with given parameters and context */
    Object execute(Map<String, Object> params, ActionContext context);
}
