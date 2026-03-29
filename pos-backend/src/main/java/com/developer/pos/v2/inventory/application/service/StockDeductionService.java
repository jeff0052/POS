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
import org.springframework.stereotype.Service;
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
    @Transactional
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
        Set<Long> deductableSkuIds = allSkuIds.stream()
                .filter(id -> skuMap.containsKey(id) && skuMap.get(id).isRequiresStockDeduct())
                .collect(Collectors.toSet());
        if (deductableSkuIds.isEmpty()) return;

        // 3. Compute total deduction per inventoryItemId using ConsumptionCalculationService
        Map<Long, BigDecimal> deductionByItemId = new HashMap<>();
        for (SubmittedOrderItemEntity orderItem : allItems) {
            if (!deductableSkuIds.contains(orderItem.getSkuId())) continue;
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

    private void deductItem(Long storeId, Long inventoryItemId, BigDecimal totalDeduct) {
        InventoryItemEntity item = inventoryItemRepository.findById(inventoryItemId).orElse(null);
        if (item == null) {
            log.warn("StockDeductionService: inventoryItem {} not found, skipping", inventoryItemId);
            return;
        }

        List<InventoryBatchEntity> batches = batchRepository
                .findByInventoryItemIdAndBatchStatusOrderByExpiryDateAscIdAsc(inventoryItemId, "ACTIVE");

        BigDecimal remaining = totalDeduct;

        // Deduct from FIFO batches
        for (InventoryBatchEntity batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal deductFromBatch = remaining.min(batch.getRemainingQty());
            batch.deductRemainingQty(deductFromBatch);
            if (batch.getRemainingQty().compareTo(BigDecimal.ZERO) == 0) {
                batch.exhaust();
            }

            item.deductStock(deductFromBatch);
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
