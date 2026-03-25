package com.developer.pos.v2.mcp;

import com.developer.pos.v2.mcp.model.RiskLevel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpToolRegistryIntegrationTest {
    @Test
    void registrySupportsAllDomainCategories() {
        var registry = new McpToolRegistry();
        // Simulate what tool classes do
        registry.register(new McpToolRegistry.ToolDefinition("list_products", "d", "catalog", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("toggle_sku", "d", "catalog", "ACTION", RiskLevel.MEDIUM, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("list_members", "d", "member", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("get_churn", "d", "member", "ANALYZE", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("get_orders", "d", "order", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("get_revenue", "d", "settlement", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("get_summary", "d", "report", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("get_stores", "d", "store", "QUERY", null, p -> "ok"));
        registry.register(new McpToolRegistry.ToolDefinition("list_promos", "d", "promotion", "QUERY", null, p -> "ok"));

        // All 7 domains present
        var domains = registry.listTools().stream()
            .map(McpToolRegistry.ToolDefinition::domain)
            .distinct().sorted().toList();
        assertEquals(7, domains.size());
        assertTrue(domains.containsAll(java.util.List.of("catalog", "member", "order", "promotion", "report", "settlement", "store")));

        // Has all 3 categories
        var categories = registry.listTools().stream()
            .map(McpToolRegistry.ToolDefinition::category)
            .distinct().sorted().toList();
        assertTrue(categories.contains("QUERY"));
        assertTrue(categories.contains("ANALYZE"));
        assertTrue(categories.contains("ACTION"));

        // Tool execution works
        assertEquals("ok", registry.getTool("list_products").get().handler().apply(null));
    }
}
