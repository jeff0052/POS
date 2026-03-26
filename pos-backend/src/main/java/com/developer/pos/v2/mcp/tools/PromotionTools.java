package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.promotion.application.service.PromotionApplicationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("promotionListRules")
class ListPromotionRules implements McpTool {
    private final PromotionApplicationService promotionService;
    ListPromotionRules(PromotionApplicationService s) { this.promotionService = s; }

    @Override public String name() { return "promotion.list_rules"; }
    @Override public String description() { return "List all promotion rules for a store (active, expired, disabled)"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "storeId", Map.of("type", "number")
        ), "required", List.of("storeId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long storeId = ((Number) params.get("storeId")).longValue();
        return promotionService.listActiveRules(storeId);
    }
}

@Component("promotionApplyBest")
class ApplyBestPromotion implements McpTool {
    private final PromotionApplicationService promotionService;
    ApplyBestPromotion(PromotionApplicationService s) { this.promotionService = s; }

    @Override public String name() { return "promotion.apply_best"; }
    @Override public String description() { return "Evaluate and apply the best matching promotion to an active order"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "activeOrderId", Map.of("type", "string")
        ), "required", List.of("activeOrderId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        return promotionService.applyBestPromotion((String) params.get("activeOrderId"));
    }
}
