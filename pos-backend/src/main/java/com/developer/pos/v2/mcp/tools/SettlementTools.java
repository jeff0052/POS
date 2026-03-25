package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SettlementTools {

    private final McpToolRegistry registry;
    private final JpaSettlementRecordRepository settlementRecordRepository;

    public SettlementTools(
            McpToolRegistry registry,
            JpaSettlementRecordRepository settlementRecordRepository
    ) {
        this.registry = registry;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_daily_revenue",
                "Get total daily revenue (all time) for a store. Params: storeId (Long).",
                "settlement",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    List<SettlementRecordEntity> records = settlementRecordRepository.findAll().stream()
                            .filter(r -> storeId.equals(r.getStoreId()))
                            .toList();
                    long totalRevenueCents = records.stream()
                            .mapToLong(SettlementRecordEntity::getCollectedAmountCents)
                            .sum();
                    return Map.of(
                            "storeId", storeId,
                            "totalSettlements", records.size(),
                            "totalRevenueCents", totalRevenueCents
                    );
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_payment_breakdown",
                "Get revenue breakdown by payment method for a store. Params: storeId (Long).",
                "settlement",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    List<SettlementRecordEntity> records = settlementRecordRepository.findAll().stream()
                            .filter(r -> storeId.equals(r.getStoreId()))
                            .toList();
                    Map<String, Long> breakdown = records.stream()
                            .collect(Collectors.groupingBy(
                                    r -> r.getPaymentMethod() != null ? r.getPaymentMethod() : "UNKNOWN",
                                    Collectors.summingLong(SettlementRecordEntity::getCollectedAmountCents)
                            ));
                    return Map.of("storeId", storeId, "breakdown", breakdown);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_refund_history",
                "Get settlement records with status != SETTLED (refunds / failed) for a store. " +
                        "Params: storeId (Long).",
                "settlement",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    // TODO: when a dedicated refund entity is added, query it here.
                    // For now, return non-SETTLED settlement records as a proxy.
                    List<SettlementRecordEntity> records = settlementRecordRepository.findAll().stream()
                            .filter(r -> storeId.equals(r.getStoreId()))
                            .filter(r -> !"SETTLED".equals(r.getFinalStatus()))
                            .toList();
                    return records.stream()
                            .map(r -> Map.of(
                                    "settlementNo", r.getSettlementNo(),
                                    "tableId", r.getTableId(),
                                    "paymentMethod", r.getPaymentMethod() != null ? r.getPaymentMethod() : "UNKNOWN",
                                    "finalStatus", r.getFinalStatus(),
                                    "payableAmountCents", r.getPayableAmountCents(),
                                    "collectedAmountCents", r.getCollectedAmountCents()
                            ))
                            .toList();
                }
        ));
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
