package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.RiskLevel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpToolRegistryTest {
    @Test
    void registersAndRetrievesTool() {
        var registry = new McpToolRegistry();
        registry.register(new McpToolRegistry.ToolDefinition(
            "test_tool", "desc", "test", "QUERY", null, params -> "result"
        ));
        assertTrue(registry.getTool("test_tool").isPresent());
        assertEquals("result", registry.getTool("test_tool").get().handler().apply(null));
    }

    @Test
    void listByDomainFiltersCorrectly() {
        var registry = new McpToolRegistry();
        registry.register(new McpToolRegistry.ToolDefinition("a", "d", "catalog", "QUERY", null, p -> null));
        registry.register(new McpToolRegistry.ToolDefinition("b", "d", "member", "QUERY", null, p -> null));
        registry.register(new McpToolRegistry.ToolDefinition("c", "d", "catalog", "ACTION", RiskLevel.MEDIUM, p -> null));
        assertEquals(2, registry.listByDomain("catalog").size());
        assertEquals(1, registry.listByDomain("member").size());
        assertEquals(0, registry.listByDomain("nonexistent").size());
    }

    @Test
    void actionToolsHaveRiskLevel() {
        var registry = new McpToolRegistry();
        registry.register(new McpToolRegistry.ToolDefinition(
            "action_tool", "desc", "promo", "ACTION", RiskLevel.HIGH, p -> null
        ));
        var tool = registry.getTool("action_tool").orElseThrow();
        assertEquals(RiskLevel.HIGH, tool.riskLevel());
    }
}
