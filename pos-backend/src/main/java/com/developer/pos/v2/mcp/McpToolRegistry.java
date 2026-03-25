package com.developer.pos.v2.mcp;

import com.developer.pos.v2.mcp.model.RiskLevel;
import java.util.*;
import java.util.function.Function;

public class McpToolRegistry {

    public record ToolDefinition(
        String name,
        String description,
        String domain,
        String category,
        RiskLevel riskLevel,
        Function<Map<String, Object>, Object> handler
    ) {}

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public Optional<ToolDefinition> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<ToolDefinition> listTools() {
        return List.copyOf(tools.values());
    }

    public List<ToolDefinition> listByDomain(String domain) {
        return tools.values().stream()
            .filter(t -> t.domain().equals(domain))
            .toList();
    }
}
