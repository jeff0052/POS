package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.report.application.service.ReportReadService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReportTools {

    private final McpToolRegistry registry;
    private final ReportReadService reportReadService;
    private final JpaSubmittedOrderRepository submittedOrderRepository;

    public ReportTools(
            McpToolRegistry registry,
            ReportReadService reportReadService,
            JpaSubmittedOrderRepository submittedOrderRepository
    ) {
        this.registry = registry;
        this.reportReadService = reportReadService;
        this.submittedOrderRepository = submittedOrderRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_daily_summary",
                "Get daily revenue and order count summary for a store. Params: storeId (Long).",
                "report",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    return reportReadService.getDailySummary(storeId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_product_ranking",
                "Rank products by quantity sold for a store based on settled submitted orders. " +
                        "Params: storeId (Long), limit (Integer, default 10).",
                "report",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    int limit = params.containsKey("limit") ? ((Number) params.get("limit")).intValue() : 10;

                    List<SubmittedOrderEntity> settledOrders = submittedOrderRepository
                            .findAllByStoreIdOrderByIdDesc(storeId)
                            .stream()
                            .filter(o -> "SETTLED".equals(o.getSettlementStatus()))
                            .toList();

                    Map<String, Long> skuQuantityMap = settledOrders.stream()
                            .flatMap(o -> o.getItems().stream())
                            .collect(Collectors.groupingBy(
                                    item -> item.getSkuNameSnapshot() != null ? item.getSkuNameSnapshot() : "UNKNOWN",
                                    Collectors.summingLong(item -> (long) item.getQuantity())
                            ));

                    return skuQuantityMap.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                            .limit(limit)
                            .map(e -> Map.of("skuName", e.getKey(), "totalQuantity", e.getValue()))
                            .toList();
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "compare_periods",
                "Compare revenue and order count between two stores or summarize overall stats. " +
                        "Params: storeId1 (Long), storeId2 (Long).",
                "report",
                "QUERY",
                null,
                params -> {
                    Long storeId1 = toLong(params.get("storeId1"));
                    Long storeId2 = toLong(params.get("storeId2"));
                    var summary1 = reportReadService.getDailySummary(storeId1);
                    var summary2 = reportReadService.getDailySummary(storeId2);
                    return Map.of(
                            "store1", Map.of("storeId", storeId1, "summary", summary1),
                            "store2", Map.of("storeId", storeId2, "summary", summary2)
                    );
                }
        ));
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
