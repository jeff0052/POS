package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.order.application.dto.ActiveTableOrderDto;
import com.developer.pos.v2.order.application.query.GetActiveTableOrderQuery;
import com.developer.pos.v2.order.application.service.ActiveTableOrderApplicationService;
import com.developer.pos.v2.order.application.service.MerchantOrderReadService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("orderGetActive")
class GetActiveOrder implements McpTool {
    private final ActiveTableOrderApplicationService orderService;
    GetActiveOrder(ActiveTableOrderApplicationService orderService) { this.orderService = orderService; }

    @Override public String name() { return "order.get_active"; }
    @Override public String description() { return "Get the current active table order for a specific table"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number"), "tableId", Map.of("type", "number")
        ), "required", List.of("storeId", "tableId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        Long tableId = ((Number) params.get("tableId")).longValue();
        return orderService.getActiveTableOrder(new GetActiveTableOrderQuery(storeId, tableId));
    }
}

@Component("orderListAll")
class ListOrders implements McpTool {
    private final MerchantOrderReadService readService;
    ListOrders(MerchantOrderReadService readService) { this.readService = readService; }

    @Override public String name() { return "order.list_all"; }
    @Override public String description() { return "List all orders (active + submitted) for a store, sorted by time"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        return readService.getOrders(storeId);
    }
}

@Component("orderSubmitToKitchen")
class SubmitToKitchen implements McpTool {
    private final ActiveTableOrderApplicationService orderService;
    SubmitToKitchen(ActiveTableOrderApplicationService orderService) { this.orderService = orderService; }

    @Override public String name() { return "order.submit_to_kitchen"; }
    @Override public String description() { return "Submit a draft active order to the kitchen"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "activeOrderId", Map.of("type", "string")
        ), "required", List.of("activeOrderId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        return orderService.submitToKitchen((String) params.get("activeOrderId"));
    }
}

@Component("orderMoveToSettlement")
class MoveToSettlement implements McpTool {
    private final ActiveTableOrderApplicationService orderService;
    MoveToSettlement(ActiveTableOrderApplicationService orderService) { this.orderService = orderService; }

    @Override public String name() { return "order.move_to_settlement"; }
    @Override public String description() { return "Move an active order to pending settlement status"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "activeOrderId", Map.of("type", "string")
        ), "required", List.of("activeOrderId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        return orderService.moveToSettlement((String) params.get("activeOrderId"));
    }
}
