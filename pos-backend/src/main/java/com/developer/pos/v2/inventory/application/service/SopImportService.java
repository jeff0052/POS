package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.inventory.application.dto.SopCsvRow;
import com.developer.pos.v2.inventory.application.dto.SopImportBatchDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.SopImportBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaSopImportBatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SopImportService implements UseCase {

    private static final Logger log = LoggerFactory.getLogger(SopImportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JpaSopImportBatchRepository batchRepository;
    private final JpaRecipeRepository recipeRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final SopCsvParser csvParser;
    private final StoreAccessEnforcer enforcer;

    public SopImportService(JpaSopImportBatchRepository batchRepository,
                             JpaRecipeRepository recipeRepository,
                             JpaSkuRepository skuRepository,
                             JpaInventoryItemRepository inventoryItemRepository,
                             SopCsvParser csvParser,
                             StoreAccessEnforcer enforcer) {
        this.batchRepository = batchRepository;
        this.recipeRepository = recipeRepository;
        this.skuRepository = skuRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.csvParser = csvParser;
        this.enforcer = enforcer;
    }

    @Transactional
    public SopImportBatchDto importCsv(Long storeId, String fileName, String csvContent) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("RECIPE_MANAGE");

        SopImportBatchEntity batch = new SopImportBatchEntity(
            storeId, fileName, null, AuthContext.current().userId());
        batch = batchRepository.save(batch);

        SopCsvParser.ParseResult parseResult = csvParser.parse(csvContent);
        List<SopCsvParser.RowError> allErrors = new ArrayList<>(parseResult.errors());

        List<SopCsvRow> dbValidRows = new ArrayList<>();
        Map<String, Long> itemCodeToId = new HashMap<>();

        for (SopCsvRow row : parseResult.validRows()) {
            if (!skuRepository.existsById(row.skuId())) {
                allErrors.add(new SopCsvParser.RowError(row.rowNumber(), "sku_id",
                    "SKU not found: " + row.skuId()));
                continue;
            }
            Long itemId = itemCodeToId.get(row.inventoryItemCode());
            if (itemId == null) {
                Optional<InventoryItemEntity> itemOpt =
                    inventoryItemRepository.findByStoreIdAndItemCode(storeId, row.inventoryItemCode());
                if (itemOpt.isEmpty()) {
                    allErrors.add(new SopCsvParser.RowError(row.rowNumber(), "inventory_item_code",
                        "Inventory item not found in store: " + row.inventoryItemCode()));
                    continue;
                }
                itemId = itemOpt.get().getId();
                itemCodeToId.put(row.inventoryItemCode(), itemId);
            }
            dbValidRows.add(row);
        }

        int totalRows = parseResult.validRows().size() + parseResult.errors().size();
        String errorJson = serializeErrors(allErrors);
        batch.markValidated(totalRows, allErrors.size(), errorJson);

        batch.startImport();
        Set<Long> processedSkuIds = new HashSet<>();
        int successCount = 0;

        for (SopCsvRow row : dbValidRows) {
            Long itemId = itemCodeToId.get(row.inventoryItemCode());
            if (processedSkuIds.add(row.skuId())) {
                recipeRepository.deleteBySkuId(row.skuId());
            }
            RecipeEntity recipe = RecipeEntity.create(
                row.skuId(), itemId, row.consumptionQty(),
                row.consumptionUnit(), row.baseMultiplier(), row.notes());
            recipeRepository.save(recipe);
            successCount++;
        }

        batch.completeImport(successCount);
        batchRepository.save(batch);
        return toDto(batch);
    }

    @Transactional(readOnly = true)
    public List<SopImportBatchDto> listBatches(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("RECIPE_MANAGE");
        return batchRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
            .stream().map(this::toDto).toList();
    }

    private String serializeErrors(List<SopCsvParser.RowError> errors) {
        if (errors.isEmpty()) return null;
        try { return MAPPER.writeValueAsString(errors); }
        catch (Exception e) { return "[{\"error\":\"serialization failed\"}]"; }
    }

    private SopImportBatchDto toDto(SopImportBatchEntity e) {
        return new SopImportBatchDto(e.getId(), e.getStoreId(), e.getFileName(),
            e.getTotalRows(), e.getSuccessRows(), e.getErrorRows(),
            e.getBatchStatus(), e.getErrorDetails(), e.getCreatedAt());
    }
}
