package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles single-item stock deduction in its own transaction (REQUIRES_NEW).
 * This ensures that on optimistic lock retry, the Hibernate session is fresh
 * and re-reads return current DB state instead of stale cached entities.
 */
@Service
class StockDeductionItemWorker {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionItemWorker.class);

    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final JpaInventoryMovementRepository movementRepository;

    StockDeductionItemWorker(
            JpaInventoryBatchRepository batchRepository,
            JpaInventoryItemRepository inventoryItemRepository,
            JpaInventoryMovementRepository movementRepository
    ) {
        this.batchRepository = batchRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductItem(Long storeId, Long inventoryItemId, BigDecimal totalDeduct) {
        // L7: Zero deduction triggers unnecessary DB queries
        if (totalDeduct.compareTo(BigDecimal.ZERO) <= 0) return;

        InventoryItemEntity item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalStateException(
                    "StockDeductionService: inventoryItem " + inventoryItemId
                    + " not found — recipe references a missing item"));

        List<InventoryBatchEntity> batches = batchRepository
                .findActiveByStoreAndItemFifo(storeId, inventoryItemId, "ACTIVE");

        BigDecimal remaining = totalDeduct;

        // Deduct from FEFO batches (First Expiry, First Out)
        for (InventoryBatchEntity batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal deductFromBatch = remaining.min(batch.getRemainingQty());
            batch.deductRemainingQty(deductFromBatch);
            // L6: Removed redundant batch.exhaust() — deductRemainingQty already auto-exhausts when remainingQty hits 0

            batchRepository.save(batch);

            item.deductStock(deductFromBatch);
            // afterQty reflects stock after each batch deduction, not the final settled stock
            movementRepository.save(new InventoryMovementEntity(
                    storeId, inventoryItemId, batch.getId(),
                    "SALE_DEDUCT", deductFromBatch.negate(), batch.getUnitCostCents(),
                    item.getCurrentStock(), "SETTLEMENT", null
            ));
            remaining = remaining.subtract(deductFromBatch);
        }

        // If batches exhausted but still need to deduct — allow negative stock with audit
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("StockDeductionService: inventoryItem {} insufficient batch stock, going negative by {}",
                    inventoryItemId, remaining);
            item.deductStock(remaining);
            movementRepository.save(new InventoryMovementEntity(
                    storeId, inventoryItemId, null,
                    "SALE_DEDUCT", remaining.negate(), null,
                    item.getCurrentStock(), "SETTLEMENT", null,
                    "Insufficient batch stock — stock went negative"
            ));
        }

        inventoryItemRepository.save(item);
    }
}
