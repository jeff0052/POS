package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.report.application.service.ReportReadService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
                "storeId", Map.of("type", "number"),
                "date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format (default: today)")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        String dateStr = (String) params.get("date");
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        return reportService.getDailySummary(storeId, date);
    }
}

@Component("reportSalesSummary")
class GetSalesSummary implements McpTool {
    private final ReportReadService reportService;
    GetSalesSummary(ReportReadService s) { this.reportService = s; }

    @Override public String name() { return "report.sales_summary"; }
    @Override public String description() { return "Get sales summary with top-selling items, revenue breakdown for a date range"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number"),
                "startDate", Map.of("type", "string", "description", "Start date YYYY-MM-DD"),
                "endDate", Map.of("type", "string", "description", "End date YYYY-MM-DD")
        ), "required", List.of("storeId", "startDate", "endDate"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        LocalDate start = LocalDate.parse((String) params.get("startDate"));
        LocalDate end = LocalDate.parse((String) params.get("endDate"));
        return reportService.getSalesSummary(storeId, start, end);
    }
}
