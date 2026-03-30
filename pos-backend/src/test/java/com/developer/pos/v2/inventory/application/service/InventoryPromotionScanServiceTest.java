package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryDrivenPromotionRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryPromotionScanServiceTest {

    @Mock JpaInventoryBatchRepository batchRepository;
    @Mock JpaInventoryItemRepository itemRepository;
    @Mock JpaRecipeRepository recipeRepository;
    @Mock JpaInventoryDrivenPromotionRepository promotionRepository;

    private InventoryPromotionScanService buildService() {
        return new InventoryPromotionScanService(batchRepository, itemRepository,
            recipeRepository, promotionRepository);
    }

    private InventoryBatchEntity makeBatch(Long id, Long storeId, Long itemId,
                                            LocalDate expiryDate, BigDecimal remainingQty) {
        try {
            Constructor<InventoryBatchEntity> ctor = InventoryBatchEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            InventoryBatchEntity b = ctor.newInstance();
            setField(b, "id", id); setField(b, "storeId", storeId);
            setField(b, "inventoryItemId", itemId); setField(b, "expiryDate", expiryDate);
            setField(b, "remainingQty", remainingQty); setField(b, "batchStatus", "ACTIVE");
            setField(b, "unit", "kg");
            return b;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private InventoryItemEntity makeItem(Long id, Long storeId, BigDecimal currentStock, BigDecimal safetyStock) {
        InventoryItemEntity item = new InventoryItemEntity(storeId, "ITEM-" + id, "Item " + id, "kg", safetyStock);
        try { setField(item, "id", id); setField(item, "currentStock", currentStock); } catch (Exception e) { throw new RuntimeException(e); }
        return item;
    }

    private RecipeEntity makeRecipe(Long id, Long skuId, Long inventoryItemId) {
        try {
            Constructor<RecipeEntity> ctor = RecipeEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            RecipeEntity r = ctor.newInstance();
            setField(r, "id", id); setField(r, "skuId", skuId);
            setField(r, "inventoryItemId", inventoryItemId);
            return r;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true); f.set(obj, value);
    }

    @Test
    void nearExpiry_3days_generates30percentDraft() {
        InventoryBatchEntity batch = makeBatch(1L, 10L, 100L,
            LocalDate.now().plusDays(2), BigDecimal.TEN);
        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(false);
        RecipeEntity recipe = makeRecipe(1L, 50L, 100L);
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of(recipe));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanNearExpiry(10L);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).triggerType()).isEqualTo("NEAR_EXPIRY");
        assertThat(drafts.get(0).suggestedDiscountPercent()).isEqualByComparingTo("30.00");
    }

    @Test
    void nearExpiry_7days_generates20percentDraft() {
        InventoryBatchEntity batch = makeBatch(1L, 10L, 100L,
            LocalDate.now().plusDays(5), BigDecimal.TEN);
        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(false);
        RecipeEntity recipe = makeRecipe(1L, 50L, 100L);
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of(recipe));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanNearExpiry(10L);

        assertThat(drafts.get(0).suggestedDiscountPercent()).isEqualByComparingTo("20.00");
    }

    @Test
    void nearExpiry_existingDraft_skipsItem() {
        InventoryBatchEntity batch = makeBatch(1L, 10L, 100L,
            LocalDate.now().plusDays(2), BigDecimal.TEN);
        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(true);

        List<InventoryDrivenPromotionDto> drafts = buildService().scanNearExpiry(10L);

        assertThat(drafts).isEmpty();
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void overstock_generates15percentDraft() {
        InventoryItemEntity item = makeItem(100L, 10L, new BigDecimal("50"), new BigDecimal("10"));
        when(itemRepository.findOverstock(eq(10L), any(BigDecimal.class)))
            .thenReturn(List.of(item));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(false);
        RecipeEntity recipe = makeRecipe(1L, 50L, 100L);
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of(recipe));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanOverstock(10L);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).triggerType()).isEqualTo("OVERSTOCK");
        assertThat(drafts.get(0).suggestedDiscountPercent()).isEqualByComparingTo("15.00");
    }

    @Test
    void overstock_noRecipesForItem_skips() {
        InventoryItemEntity item = makeItem(100L, 10L, new BigDecimal("50"), new BigDecimal("10"));
        when(itemRepository.findOverstock(eq(10L), any(BigDecimal.class)))
            .thenReturn(List.of(item));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(false);
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of());

        List<InventoryDrivenPromotionDto> drafts = buildService().scanOverstock(10L);

        assertThat(drafts).isEmpty();
    }

    @Test
    void scanAll_combinesNearExpiryAndOverstock() {
        InventoryBatchEntity batch = makeBatch(1L, 10L, 100L,
            LocalDate.now().plusDays(2), BigDecimal.TEN);
        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch));
        InventoryItemEntity item = makeItem(200L, 10L, new BigDecimal("50"), new BigDecimal("10"));
        when(itemRepository.findOverstock(eq(10L), any(BigDecimal.class)))
            .thenReturn(List.of(item));
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of(makeRecipe(1L, 50L, 100L)));
        when(recipeRepository.findByInventoryItemId(200L)).thenReturn(List.of(makeRecipe(2L, 60L, 200L)));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(anyLong(), anyLong(), eq("DRAFT")))
            .thenReturn(false);
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanAll(10L);

        assertThat(drafts).hasSize(2);
    }
}
