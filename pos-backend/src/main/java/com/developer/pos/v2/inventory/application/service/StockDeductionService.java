package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.inventory.application.dto.ConsumptionResult;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockDeductionService {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionService.class);

    private final JpaSkuRepository skuRepository;
    private final ConsumptionCalculationService consumptionCalculationService;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryMovementRepository movementRepository;

    public StockDeductionService(
            JpaSkuRepository skuRepository,
            ConsumptionCalculationService consumptionCalculationService,
            JpaInventoryItemRepository inventoryItemRepository,
            JpaInventoryBatchRepository batchRepository,
            JpaInventoryMovementRepository movementRepository
    ) {
        this.skuRepository = skuRepository;
        this.consumptionCalculationService = consumptionCalculationService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
    }

    /**
     * Deduct inventory for a list of settled orders.
     * Called from CashierSettlementApplicationService after orders are marked SETTLED.
     * Runs in the caller's transaction.
     */
    // TODO: To enable per-order movement traceability, deduct per-order instead of aggregating.
    // Current design aggregates all orders' consumption per inventory item for efficiency.
    @Transactional(propagation = Propagation.MANDATORY)
    public void deductForOrders(Long storeId, List<SubmittedOrderEntity> settledOrders) {
        // 1. Collect all order items
        List<SubmittedOrderItemEntity> allItems = settledOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .toList();
        if (allItems.isEmpty()) return;

        // 2. Load SKUs — filter to those requiring stock deduction
        Set<Long> allSkuIds = allItems.stream()
                .map(SubmittedOrderItemEntity::getSkuId)
                .collect(Collectors.toSet());
        Map<Long, SkuEntity> skuMap = skuRepository.findAllById(allSkuIds).stream()
                .collect(Collectors.toMap(SkuEntity::getId, s -> s));
        Set<Long> deductibleSkuIds = allSkuIds.stream()
                .filter(id -> skuMap.containsKey(id) && skuMap.get(id).isRequiresStockDeduct())
                .collect(Collectors.toSet());
        if (deductibleSkuIds.isEmpty()) return;

        // 3. Compute total deduction per inventoryItemId using ConsumptionCalculationService
        Map<Long, BigDecimal> deductionByItemId = new HashMap<>();
        for (SubmittedOrderItemEntity orderItem : allItems) {
            if (!deductibleSkuIds.contains(orderItem.getSkuId())) continue;
            List<ConsumptionResult> consumptions = consumptionCalculationService.calculate(
                orderItem.getSkuId(), orderItem.getQuantity(), orderItem.getOptionSnapshotJson());
            for (ConsumptionResult c : consumptions) {
                deductionByItemId.merge(c.inventoryItemId(), c.qty(), BigDecimal::add);
            }
        }

        // 4. FIFO deduct each inventoryItem
        for (Map.Entry<Long, BigDecimal> entry : deductionByItemId.entrySet()) {
            deductItem(storeId, entry.getKey(), entry.getValue());
        }
    }

    // NOTE: Concurrent safety via @Version optimistic locking on InventoryBatchEntity + InventoryItemEntity (V099 migration)
    // C5: Retry up to 3 times on optimistic lock conflicts (concurrent settlement windows)
    private void deductItem(Long storeId, Long inventoryItemId, BigDecimal totalDeduct) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                doDeductItem(storeId, inventoryItemId, totalDeduct);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == 3) throw e;
                log.warn("StockDeductionService: optimistic lock conflict for item {}, retrying (attempt {})",
                        inventoryItemId, attempt);
                // Retry: re-read fresh state from DB
            }
        }
    }

    private void doDeductItem(Long storeId, Long inventoryItemId, BigDecimal totalDeduct) {
        InventoryItemEntity item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalStateException(
                    "StockDeductionService: inventoryItem " + inventoryItemId
                    + " not found — recipe references a missing item"));

        List<InventoryBatchEntity> batches = batchRepository
                .findActiveByStoreAndItemFifo(storeId, inventoryItemId, "ACTIVE");

        BigDecimal remaining = totalDeduct;

        // Deduct from FIFO batches
        for (InventoryBatchEntity batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal deductFromBatch = remaining.min(batch.getRemainingQty());
            batch.deductRemainingQty(deductFromBatch);
            if (batch.getRemainingQty().compareTo(BigDecimal.ZERO) == 0) {
                batch.exhaust();
            }

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

        // C6: Single save is sufficient — JPA dirty checking within @Transactional tracks
        // the managed entity, and @Version on both batch and item provides conflict detection.
        // Each batch.save() above persists batch state immediately; the final item save
        // flushes the accumulated stock changes at transaction commit.
        inventoryItemRepository.save(item);
    }
}
