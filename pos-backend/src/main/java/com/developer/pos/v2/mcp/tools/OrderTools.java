package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.order.application.service.MerchantOrderReadService;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderTools {

    private final McpToolRegistry registry;
    private final MerchantOrderReadService orderReadService;
    private final JpaStoreTableRepository storeTableRepository;

    public OrderTools(
            McpToolRegistry registry,
            MerchantOrderReadService orderReadService,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.registry = registry;
        this.orderReadService = orderReadService;
        this.storeTableRepository = storeTableRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_active_orders",
                "Get all active and submitted orders for a store. Params: storeId (Long).",
                "order",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    return orderReadService.getOrders(storeId).stream()
                            .filter(o -> !"PAID".equals(o.paymentMethod()))
                            .toList();
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_order_history",
                "Get all orders (including settled) for a store. Params: storeId (Long).",
                "order",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    return orderReadService.getOrders(storeId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_table_status",
                "Get status of all tables in a store. Params: storeId (Long).",
                "order",
                "QUERY",
                null,
                params -> {
                    Long storeId = toLong(params.get("storeId"));
                    List<StoreTableEntity> tables = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId);
                    return tables.stream()
                            .map(t -> Map.of(
                                    "tableId", t.getId(),
                                    "tableCode", t.getTableCode(),
                                    "tableName", t.getTableName(),
                                    "tableStatus", t.getTableStatus()
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
