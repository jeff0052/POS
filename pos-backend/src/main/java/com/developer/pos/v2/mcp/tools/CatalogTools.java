package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.catalog.application.service.AdminCatalogReadService;
import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("catalogListProducts")
class ListProducts implements McpTool {
    private final AdminCatalogReadService catalogService;
    ListProducts(AdminCatalogReadService catalogService) { this.catalogService = catalogService; }

    @Override public String name() { return "catalog.list_products"; }
    @Override public String description() { return "List all products and SKUs for a store, grouped by category"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number", "description", "Store ID")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        return catalogService.getFullCatalog(storeId);
    }
}

@Component("catalogGetMenu")
class GetQrMenu implements McpTool {
    private final com.developer.pos.v2.catalog.application.service.QrMenuApplicationService menuService;
    GetQrMenu(com.developer.pos.v2.catalog.application.service.QrMenuApplicationService menuService) { this.menuService = menuService; }

    @Override public String name() { return "catalog.get_menu"; }
    @Override public String description() { return "Get the customer-facing menu for a store (categories + available SKUs)"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeCode", Map.of("type", "string", "description", "Store code")
        ), "required", List.of("storeCode"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        return menuService.getMenu((String) params.get("storeCode"));
    }
}
