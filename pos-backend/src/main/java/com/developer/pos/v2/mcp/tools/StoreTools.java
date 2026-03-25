package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StoreTools {

    private final McpToolRegistry registry;
    private final JpaStoreRepository storeRepository;
    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public StoreTools(
            McpToolRegistry registry,
            JpaStoreRepository storeRepository,
            JpaStoreLookupRepository storeLookupRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.registry = registry;
        this.storeRepository = storeRepository;
        this.storeLookupRepository = storeLookupRepository;
        this.storeTableRepository = storeTableRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_store_list",
                "List all stores. No params required.",
                "store",
                "QUERY",
                null,
                params -> {
                    List<StoreEntity> stores = storeRepository.findAll();
                    return stores.stream()
                            .map(s -> Map.of(
                                    "storeId", s.getId(),
                                    "storeCode", s.getStoreCode(),
                                    "storeName", s.getStoreName(),
                                    "merchantId", s.getMerchantId()
                            ))
                            .toList();
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_table_layout",
                "Get all tables for a store with their current status. Params: storeId (Long).",
                "store",
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

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_store_config",
                "Get configuration/info for a specific store by storeCode. Params: storeCode (String).",
                "store",
                "QUERY",
                null,
                params -> {
                    String storeCode = (String) params.get("storeCode");
                    StoreEntity store = storeLookupRepository.findByStoreCode(storeCode)
                            .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeCode));
                    List<StoreTableEntity> tables = storeTableRepository.findAllByStoreIdOrderByIdAsc(store.getId());
                    return Map.of(
                            "storeId", store.getId(),
                            "storeCode", store.getStoreCode(),
                            "storeName", store.getStoreName(),
                            "merchantId", store.getMerchantId(),
                            "tableCount", tables.size()
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
