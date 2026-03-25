package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.ActionContext;
import com.developer.pos.v2.mcp.model.RiskLevel;
import com.developer.pos.v2.promotion.application.dto.UpsertPromotionRuleDto;
import com.developer.pos.v2.promotion.application.service.PromotionApplicationService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class PromotionTools {

    private final McpToolRegistry registry;
    private final PromotionApplicationService promotionService;
    private final ActionLogService actionLogService;

    public PromotionTools(
            McpToolRegistry registry,
            PromotionApplicationService promotionService,
            ActionLogService actionLogService
    ) {
        this.registry = registry;
        this.promotionService = promotionService;
        this.actionLogService = actionLogService;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "list_promotions",
                "List all active promotion rules for a store. Params: storeId (Long).",
                "promotion",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    return promotionService.listActiveRules(storeId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_promotion_detail",
                "Get full detail of a promotion rule by ID. Params: ruleId (Long).",
                "promotion",
                "QUERY",
                null,
                params -> {
                    Long ruleId = toLong(params.get("ruleId"));
                    return promotionService.getRule(ruleId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "create_promotion_draft",
                "Create a new promotion rule. Params: merchantId, storeId, ruleCode, ruleName, ruleType, " +
                        "conditionType, thresholdAmountCents, rewardType, discountAmountCents, priority.",
                "promotion",
                "ACTION",
                RiskLevel.MEDIUM,
                params -> {
                    Long merchantId = toLong(params.get("merchantId"));
                    Long storeId = toLong(params.get("storeId"));
                    String ruleCode = (String) params.get("ruleCode");
                    String ruleName = (String) params.get("ruleName");
                    String ruleType = (String) params.get("ruleType");
                    String conditionType = (String) params.getOrDefault("conditionType", "THRESHOLD_AMOUNT");
                    Long thresholdAmountCents = params.containsKey("thresholdAmountCents")
                            ? toLong(params.get("thresholdAmountCents")) : null;
                    String rewardType = (String) params.getOrDefault("rewardType", "DISCOUNT_AMOUNT");
                    Long discountAmountCents = params.containsKey("discountAmountCents")
                            ? toLong(params.get("discountAmountCents")) : null;
                    int priority = params.containsKey("priority")
                            ? ((Number) params.get("priority")).intValue() : 0;

                    UpsertPromotionRuleDto command = new UpsertPromotionRuleDto(
                            merchantId,
                            storeId,
                            ruleCode,
                            ruleName,
                            ruleType,
                            "ACTIVE",
                            priority,
                            null,
                            null,
                            conditionType,
                            thresholdAmountCents,
                            rewardType,
                            discountAmountCents,
                            null,
                            null
                    );

                    Object result = promotionService.createRule(command);
                    ActionContext ctx = ActionContext.humanDefault();
                    actionLogService.log("create_promotion_draft", ctx, RiskLevel.MEDIUM, params, result);
                    return result;
                }
        ));
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
