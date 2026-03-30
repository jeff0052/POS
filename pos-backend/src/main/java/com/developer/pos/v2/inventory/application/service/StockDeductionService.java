package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.inventory.application.dto.ConsumptionResult;
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
    private final StockDeductionItemWorker worker;

    public StockDeductionService(
            JpaSkuRepository skuRepository,
            ConsumptionCalculationService consumptionCalculationService,
            StockDeductionItemWorker worker
    ) {
        this.skuRepository = skuRepository;
        this.consumptionCalculationService = consumptionCalculationService;
        this.worker = worker;
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

        // 4. FEFO deduct each inventoryItem with retry on optimistic lock conflict
        for (Map.Entry<Long, BigDecimal> entry : deductionByItemId.entrySet()) {
            deductWithRetry(storeId, entry.getKey(), entry.getValue());
        }
    }

    // M1: Each retry calls worker.deductItem() which runs in REQUIRES_NEW transaction,
    // ensuring a fresh Hibernate session on each attempt. This fixes the original issue
    // where retries within the same session would re-read stale cached entities.
    // C5: Retry up to 3 times on optimistic lock conflicts (concurrent settlement windows)
    private void deductWithRetry(Long storeId, Long inventoryItemId, BigDecimal totalDeduct) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                worker.deductItem(storeId, inventoryItemId, totalDeduct);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == 3) throw e;
                log.warn("StockDeductionService: optimistic lock conflict for item {}, retrying (attempt {})",
                        inventoryItemId, attempt);
            }
        }
    }
}
