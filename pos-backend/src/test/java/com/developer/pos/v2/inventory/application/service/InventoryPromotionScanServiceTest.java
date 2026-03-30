package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryDrivenPromotionRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryPromotionScanServiceTest {

    @Mock JpaInventoryBatchRepository batchRepository;
    @Mock JpaInventoryItemRepository itemRepository;
    @Mock JpaRecipeRepository recipeRepository;
    @Mock JpaInventoryDrivenPromotionRepository promotionRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;

    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() { if (securityMock != null) securityMock.close(); }

    private InventoryPromotionScanService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new InventoryPromotionScanService(batchRepository, itemRepository,
            recipeRepository, promotionRepository, enforcer);
    }

    private void setupActor(Long merchantId, Long storeId) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "manager", "M001", "STORE_MANAGER",
            merchantId, storeId, Set.of(storeId), Set.of("INVENTORY_VIEW"));
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(actor);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    private StoreEntity buildStore(Long merchantId) {
        try {
            Constructor<StoreEntity> ctor = StoreEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            StoreEntity store = ctor.newInstance();
            setField(store, "merchantId", merchantId);
            return store;
        } catch (Exception e) { throw new RuntimeException(e); }
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
    void nearExpiry_10days_beyondThreshold_getsDefaultDiscount() {
        // 10 days is > 7 but within the 14-day scan window, so the batch IS returned
        // by findExpiringSoon. It gets the default 10% discount tier.
        InventoryBatchEntity batch = makeBatch(1L, 10L, 100L,
            LocalDate.now().plusDays(10), BigDecimal.TEN);
        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(10L, 100L, "DRAFT"))
            .thenReturn(false);
        RecipeEntity recipe = makeRecipe(1L, 50L, 100L);
        when(recipeRepository.findByInventoryItemId(100L)).thenReturn(List.of(recipe));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanNearExpiry(10L);

        assertThat(drafts).hasSize(1);
        // >7 days gets the default 10% discount tier
        assertThat(drafts.get(0).suggestedDiscountPercent()).isEqualByComparingTo("10.00");
    }

    @Test
    void overstock_normalStockLevels_createsNoDraft() {
        // When findOverstock returns empty list (no items exceed threshold), no drafts created
        when(itemRepository.findOverstock(eq(10L), any(BigDecimal.class)))
            .thenReturn(List.of());

        List<InventoryDrivenPromotionDto> drafts = buildService().scanOverstock(10L);

        assertThat(drafts).isEmpty();
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void computeExpiryDiscount_boundaryAt3And7Days() {
        // Exactly 3 days -> 30%
        InventoryBatchEntity batch3 = makeBatch(10L, 10L, 1000L,
            LocalDate.now().plusDays(3), BigDecimal.TEN);
        // Exactly 4 days -> 20% (crosses 3-day boundary)
        InventoryBatchEntity batch4 = makeBatch(11L, 10L, 1001L,
            LocalDate.now().plusDays(4), BigDecimal.TEN);
        // Exactly 7 days -> 20%
        InventoryBatchEntity batch7 = makeBatch(12L, 10L, 1002L,
            LocalDate.now().plusDays(7), BigDecimal.TEN);
        // Exactly 8 days -> 10% (crosses 7-day boundary)
        InventoryBatchEntity batch8 = makeBatch(13L, 10L, 1003L,
            LocalDate.now().plusDays(8), BigDecimal.TEN);

        when(batchRepository.findExpiringSoon(eq(10L), any(LocalDate.class)))
            .thenReturn(List.of(batch3, batch4, batch7, batch8));
        when(promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(eq(10L), anyLong(), eq("DRAFT")))
            .thenReturn(false);
        when(recipeRepository.findByInventoryItemId(1000L)).thenReturn(List.of(makeRecipe(90L, 90L, 1000L)));
        when(recipeRepository.findByInventoryItemId(1001L)).thenReturn(List.of(makeRecipe(91L, 91L, 1001L)));
        when(recipeRepository.findByInventoryItemId(1002L)).thenReturn(List.of(makeRecipe(92L, 92L, 1002L)));
        when(recipeRepository.findByInventoryItemId(1003L)).thenReturn(List.of(makeRecipe(93L, 93L, 1003L)));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<InventoryDrivenPromotionDto> drafts = buildService().scanNearExpiry(10L);

        assertThat(drafts).hasSize(4);
        assertThat(drafts.get(0).suggestedDiscountPercent()).isEqualByComparingTo("30.00"); // 3 days
        assertThat(drafts.get(1).suggestedDiscountPercent()).isEqualByComparingTo("20.00"); // 4 days
        assertThat(drafts.get(2).suggestedDiscountPercent()).isEqualByComparingTo("20.00"); // 7 days
        assertThat(drafts.get(3).suggestedDiscountPercent()).isEqualByComparingTo("10.00"); // 8 days
    }

    @Test
    void scanAll_combinesNearExpiryAndOverstock() {
        setupActor(5L, 10L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));
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
