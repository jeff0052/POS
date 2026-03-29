package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import org.springframework.stereotype.Service;

/**
 * Stub for Task 1 — full implementation added in Task 2.
 */
@Service
public class StockDeductionService {

    private final JpaSkuRepository skuRepository;
    private final JpaRecipeRepository recipeRepository;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryMovementRepository movementRepository;

    public StockDeductionService(JpaSkuRepository skuRepository,
                                  JpaRecipeRepository recipeRepository,
                                  JpaInventoryItemRepository inventoryItemRepository,
                                  JpaInventoryBatchRepository batchRepository,
                                  JpaInventoryMovementRepository movementRepository) {
        this.skuRepository = skuRepository;
        this.recipeRepository = recipeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
    }
}
