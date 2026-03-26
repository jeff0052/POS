package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.report.application.service.ReportReadService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("reportDailySummary")
class GetDailySummary implements McpTool {
    private final ReportReadService reportService;
    GetDailySummary(ReportReadService s) { this.reportService = s; }

    @Override public String name() { return "report.daily_summary"; }
    @Override public String description() { return "Get daily sales summary for a store (total sales, order count, avg ticket, payment method breakdown)"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        return reportService.getDailySummary(storeId);
    }
}

@Component("reportSalesSummary")
class GetSalesSummary implements McpTool {
    private final ReportReadService reportService;
    GetSalesSummary(ReportReadService s) { this.reportService = s; }

    @Override public String name() { return "report.sales_summary"; }
    @Override public String description() { return "Get sales summary with top-selling items and revenue breakdown"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number"),
                "merchantId", Map.of("type", "number")
        ), "required", List.of("storeId", "merchantId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        Long merchantId = ((Number) params.get("merchantId")).longValue();
        return reportService.getSalesSummary(storeId, merchantId);
    }
}

@Component("reportOrderStateMonitor")
class GetOrderStateMonitor implements McpTool {
    private final ReportReadService reportService;
    GetOrderStateMonitor(ReportReadService s) { this.reportService = s; }

    @Override public String name() { return "report.order_state_monitor"; }
    @Override public String description() { return "Monitor current order states across all tables — identify stuck or anomalous orders"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        return reportService.getOrderStateMonitor(storeId);
    }
}
