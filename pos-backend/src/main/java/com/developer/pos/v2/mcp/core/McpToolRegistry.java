package com.developer.pos.v2.mcp.core;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Central registry of all MCP tools. Auto-discovers tools via Spring DI.
 */
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public McpToolRegistry(List<McpTool> toolBeans) {
        for (McpTool tool : toolBeans) {
            tools.put(tool.name(), tool);
        }
    }

    public McpTool get(String name) {
        return tools.get(name);
    }

    public Collection<McpTool> all() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public List<Map<String, Object>> listToolDefinitions() {
        return tools.values().stream()
                .map(tool -> Map.<String, Object>of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "inputSchema", tool.inputSchema()
                ))
                .toList();
    }
}
