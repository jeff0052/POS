package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("settlementPreview")
class GetSettlementPreview implements McpTool {
    private final CashierSettlementApplicationService settlementService;
    GetSettlementPreview(CashierSettlementApplicationService s) { this.settlementService = s; }

    @Override public String name() { return "settlement.preview"; }
    @Override public String description() { return "Get settlement preview for a table (pricing breakdown, member info, gift items)"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number"),
                "tableId", Map.of("type", "number")
        ), "required", List.of("storeId", "tableId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        Long tableId = ((Number) params.get("tableId")).longValue();
        return settlementService.getTableSettlementPreview(storeId, tableId);
    }
}
