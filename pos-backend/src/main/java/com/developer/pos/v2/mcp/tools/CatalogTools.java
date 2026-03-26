package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.catalog.application.service.AdminCatalogReadService;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaStoreSkuAvailabilityRepository;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.RiskLevel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class CatalogTools {

    private final McpToolRegistry registry;
    private final AdminCatalogReadService readService;
    private final JpaStoreSkuAvailabilityRepository availabilityRepository;

    public CatalogTools(
            McpToolRegistry registry,
            AdminCatalogReadService readService,
            JpaStoreSkuAvailabilityRepository availabilityRepository
    ) {
        this.registry = registry;
        this.readService = readService;
        this.availabilityRepository = availabilityRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "list_products",
                "List all products for a store. Params: storeCode (String).",
                "catalog",
                "QUERY",
                null,
                params -> {
                    String storeCode = (String) params.get("storeCode");
                    return readService.getProducts(storeCode);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "list_categories",
                "List all product categories for a store. Params: storeCode (String).",
                "catalog",
                "QUERY",
                null,
                params -> {
                    String storeCode = (String) params.get("storeCode");
                    return readService.getCategories(storeCode);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "toggle_sku_availability",
                "Toggle SKU availability for a store. Params: storeId (Long), skuId (Long), available (Boolean).",
                "catalog",
                "ACTION",
                RiskLevel.MEDIUM,
                this::doToggleSkuAvailability
        ));
    }

    @Transactional
    public Object doToggleSkuAvailability(Map<String, Object> params) {
        Long storeId = toLong(params.get("storeId"));
        Long skuId = toLong(params.get("skuId"));
        boolean available = Boolean.parseBoolean(String.valueOf(params.get("available")));

        StoreSkuAvailabilityEntity entity = availabilityRepository
                .findByStoreIdAndSkuId(storeId, skuId)
                .orElseGet(() -> new StoreSkuAvailabilityEntity(storeId, skuId, available));
        entity.updateAvailability(available);
        StoreSkuAvailabilityEntity saved = availabilityRepository.save(entity);

        return Map.of("storeId", storeId, "skuId", skuId, "available", saved.isAvailable());
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
