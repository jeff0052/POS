package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StockDeductionServiceTest {

    @Mock JpaSkuRepository skuRepository;
    @Mock JpaRecipeRepository recipeRepository;
    @Mock JpaInventoryItemRepository inventoryItemRepository;
    @Mock JpaInventoryBatchRepository batchRepository;
    @Mock JpaInventoryMovementRepository movementRepository;

    @InjectMocks StockDeductionService stockDeductionService;

    // Entity mutation helper tests
    @Test
    void inventoryItem_deductStock_decreasesCurrentStock() {
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛肉", "kg", new BigDecimal("5.0000"));
        item.addStock(new BigDecimal("10.0000"));
        item.deductStock(new BigDecimal("3.0000"));
        assertThat(item.getCurrentStock()).isEqualByComparingTo("7.0000");
    }

    @Test
    void inventoryBatch_deductRemainingQty_reducesQty() {
        InventoryBatchEntity batch = new InventoryBatchEntity(10L, 1L, "BATCH-001", "PURCHASE", null, new BigDecimal("5.0000"), "kg", null, null, null);
        batch.deductRemainingQty(new BigDecimal("2.0000"));
        assertThat(batch.getRemainingQty()).isEqualByComparingTo("3.0000");
        assertThat(batch.getBatchStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void inventoryBatch_exhaust_setsBatchStatusExhausted() {
        InventoryBatchEntity batch = new InventoryBatchEntity(10L, 1L, "BATCH-001", "PURCHASE", null, new BigDecimal("5.0000"), "kg", null, null, null);
        batch.deductRemainingQty(new BigDecimal("5.0000"));
        batch.exhaust();
        assertThat(batch.getBatchStatus()).isEqualTo("EXHAUSTED");
    }

    @Test
    void sku_requiresStockDeduct_defaultsTrue() {
        SkuEntity sku = new SkuEntity(1L, "SKU-001", "牛腩饭", 1800L, "ACTIVE");
        assertThat(sku.isRequiresStockDeduct()).isTrue();
    }
}
