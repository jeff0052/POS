package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.inventory.application.dto.ConsumptionResult;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockDeductionServiceTest {

    @Mock JpaSkuRepository skuRepository;
    @Mock ConsumptionCalculationService consumptionCalculationService;
    @Mock JpaInventoryItemRepository inventoryItemRepository;
    @Mock JpaInventoryBatchRepository batchRepository;
    @Mock JpaInventoryMovementRepository movementRepository;

    @InjectMocks StockDeductionService stockDeductionService;

    // ── entity mutation helpers ──────────────────────────────────────────────

    @Test
    void inventoryItem_deductStock_decreasesCurrentStock() {
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("5.0000"));
        item.addStock(new BigDecimal("10.0000"));
        item.deductStock(new BigDecimal("3.0000"));
        assertThat(item.getCurrentStock()).isEqualByComparingTo("7.0000");
    }

    @Test
    void inventoryBatch_deductRemainingQty_reducesQty() {
        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        batch.deductRemainingQty(new BigDecimal("2.0000"));
        assertThat(batch.getRemainingQty()).isEqualByComparingTo("3.0000");
        assertThat(batch.getBatchStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void inventoryBatch_exhaust_setsBatchStatusExhausted() {
        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        batch.deductRemainingQty(new BigDecimal("5.0000"));
        batch.exhaust();
        assertThat(batch.getBatchStatus()).isEqualTo("EXHAUSTED");
    }

    @Test
    void sku_requiresStockDeduct_defaultsTrue() {
        SkuEntity sku = new SkuEntity(1L, "SKU-001", "牛腩饭", 1800L, "ACTIVE");
        assertThat(sku.isRequiresStockDeduct()).isTrue();
    }

    // ── service logic ────────────────────────────────────────────────────────

    @Test
    void deductForOrders_noItems_doesNothing() {
        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of());

        stockDeductionService.deductForOrders(10L, List.of(order));

        verifyNoInteractions(skuRepository, consumptionCalculationService, inventoryItemRepository, batchRepository, movementRepository);
    }

    @Test
    void deductForOrders_skuNotRequiresDeduct_skipped() {
        SkuEntity sku = makeSkuWithDeductFlag(200L, false);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        SubmittedOrderItemEntity item = makeOrderItem(200L, 2);
        when(order.getItems()).thenReturn(List.of(item));

        stockDeductionService.deductForOrders(10L, List.of(order));

        verifyNoInteractions(consumptionCalculationService, inventoryItemRepository, batchRepository, movementRepository);
    }

    @Test
    void deductForOrders_singleSkuSingleBatch_deductsFifo() {
        // SKU 100 → recipe → inventoryItem 1, consumptionQty=0.5, baseMultiplier=1.0
        // Order: 2 units → total deduction = 2 * 0.5 * 1.0 = 1.0 kg
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(2), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("1.0000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 2)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        assertThat(batch.getRemainingQty()).isEqualByComparingTo("4.0000");
        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("4.0000");
        verify(movementRepository, times(1)).save(any(InventoryMovementEntity.class));
    }

    @Test
    void deductForOrders_twoOrdersSameItem_accumulates() {
        // 2 orders each 1 unit of SKU 100 → total 2 * 0.5 = 1.0 deducted
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(1), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("0.5000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch));

        SubmittedOrderEntity order1 = mock(SubmittedOrderEntity.class);
        when(order1.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));
        SubmittedOrderEntity order2 = mock(SubmittedOrderEntity.class);
        when(order2.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));

        stockDeductionService.deductForOrders(10L, List.of(order1, order2));

        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("4.0000");
        assertThat(batch.getRemainingQty()).isEqualByComparingTo("4.0000");  // add this
        verify(movementRepository, times(1)).save(any(InventoryMovementEntity.class));  // add this
    }

    @Test
    void deductForOrders_batchExhausted_spillsToNextBatch() {
        // Batch A: 0.3 remaining; Batch B: 5.0 remaining
        // Deduction needed: 2 units * 0.5 = 1.0 → exhaust A (0.3), deduct 0.7 from B
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(2), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("1.0000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.3000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batchA = makeBatch(1L, new BigDecimal("0.3000"));
        InventoryBatchEntity batchB = makeBatch(2L, new BigDecimal("5.0000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batchA, batchB));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 2)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        assertThat(batchA.getRemainingQty()).isEqualByComparingTo("0.0000");
        assertThat(batchA.getBatchStatus()).isEqualTo("EXHAUSTED");
        assertThat(batchB.getRemainingQty()).isEqualByComparingTo("4.3000");
        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("4.3000");
        verify(movementRepository, times(2)).save(any(InventoryMovementEntity.class));
    }

    @Test
    void deductForOrders_insufficientStock_goesNegative() {
        // Only 0.2 remaining, need 1.0 → stock goes to -0.8
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(2), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("1.0000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("0.2000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("0.2000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 2)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        // currentStock: 0.2 - 0.2 (batch) - 0.8 (overflow) = -0.8
        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("-0.8000");
    }

    @Test
    void deductForOrders_baseMultiplierApplied() {
        // consumptionQty=0.5, baseMultiplier=1.5 → 1 unit = 0.75 deducted
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(1), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("0.7500"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("4.2500");
    }

    @Test
    void deductForOrders_skuHasNoRecipe_skipped() {
        // SKU 100 requires deduct but has no recipe — should load SKU, calculate (empty), then do nothing
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));
        when(consumptionCalculationService.calculate(eq(100L), eq(2), any()))
            .thenReturn(List.of()); // no consumptions

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 2)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        verifyNoInteractions(inventoryItemRepository, batchRepository, movementRepository);
    }

    // ── I8: Zero deduction quantity ────────────────────────────────────────

    @Test
    void deductForOrders_zeroQuantity_isNoOp() {
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(0), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, BigDecimal.ZERO, "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(makeBatch(1L, new BigDecimal("5.0000"))));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 0)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        // Zero deduction: stock unchanged, no movement records created
        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("5.0000");
        verify(movementRepository, never()).save(any(InventoryMovementEntity.class));
    }

    // ── I10: OptimisticLockException retry ──────────────────────────────────

    @Test
    void deductForOrders_optimisticLockRetry_succeedsOnSecondAttempt() {
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(1), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("1.0000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        InventoryBatchEntity batch = makeBatch(1L, new BigDecimal("5.0000"));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch));

        // First save throws optimistic lock, second call succeeds
        when(batchRepository.save(any(InventoryBatchEntity.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException("conflict", null))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Re-read fresh state on retry
        InventoryItemEntity freshItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        freshItem.addStock(new BigDecimal("5.0000"));
        InventoryBatchEntity freshBatch = makeBatch(1L, new BigDecimal("5.0000"));
        when(inventoryItemRepository.findById(1L))
            .thenReturn(Optional.of(inventoryItem))
            .thenReturn(Optional.of(freshItem));
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of(batch))
            .thenReturn(List.of(freshBatch));

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        // Verify batch save was called twice (first failed, second succeeded)
        verify(batchRepository, times(2)).save(any(InventoryBatchEntity.class));
    }

    // ── I11: Missing InventoryItem ──────────────────────────────────────────

    @Test
    void deductForOrders_missingInventoryItem_throwsIllegalState() {
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(1), any()))
            .thenReturn(List.of(new ConsumptionResult(999L, new BigDecimal("1.0000"), "kg")));

        when(inventoryItemRepository.findById(999L)).thenReturn(Optional.empty());

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));

        assertThatThrownBy(() -> stockDeductionService.deductForOrders(10L, List.of(order)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("inventoryItem 999")
            .hasMessageContaining("not found");
    }

    // ── I12: Empty batch list (overflow to negative stock) ──────────────────

    @Test
    void deductForOrders_noBatches_entireAmountOverflows() {
        SkuEntity sku = makeSkuWithDeductFlag(100L, true);
        when(skuRepository.findAllById(anyCollection())).thenReturn(List.of(sku));

        when(consumptionCalculationService.calculate(eq(100L), eq(1), any()))
            .thenReturn(List.of(new ConsumptionResult(1L, new BigDecimal("2.0000"), "kg")));

        InventoryItemEntity inventoryItem = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("2.0000"));
        inventoryItem.addStock(new BigDecimal("3.0000"));
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        // No active batches at all
        when(batchRepository.findActiveByStoreAndItemFifo(10L, 1L, "ACTIVE"))
            .thenReturn(List.of());

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getItems()).thenReturn(List.of(makeOrderItem(100L, 1)));

        stockDeductionService.deductForOrders(10L, List.of(order));

        // Full amount goes through overflow path: 3.0 - 2.0 = 1.0
        assertThat(inventoryItem.getCurrentStock()).isEqualByComparingTo("1.0000");
        // Single overflow movement record with null batchId
        verify(movementRepository, times(1)).save(any(InventoryMovementEntity.class));
    }

    // ── test helpers ─────────────────────────────────────────────────────────

    private InventoryBatchEntity makeBatch(Long id, BigDecimal qty) {
        InventoryBatchEntity batch = new InventoryBatchEntity(10L, 1L, "BATCH-" + id, "PURCHASE", null, qty, "kg", null, null, null);
        try {
            Field f = InventoryBatchEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(batch, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return batch;
    }

    private SkuEntity makeSkuWithDeductFlag(Long id, boolean requiresDeduct) {
        try {
            SkuEntity sku = new SkuEntity(1L, "SKU-" + id, "Item " + id, 1800L, "ACTIVE");
            setField(sku, "id", id);
            setField(sku, "requiresStockDeduct", requiresDeduct);
            return sku;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private SubmittedOrderItemEntity makeOrderItem(Long skuId, int quantity) {
        SubmittedOrderItemEntity item = new SubmittedOrderItemEntity();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setSkuNameSnapshot("Item");
        item.setSkuCodeSnapshot("CODE");
        item.setUnitPriceSnapshotCents(1800L);
        item.setLineTotalCents(1800L * quantity);
        return item;
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try { return clazz.getDeclaredField(name); }
        catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }
}
