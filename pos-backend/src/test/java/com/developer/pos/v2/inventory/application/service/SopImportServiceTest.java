package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.SopImportBatchDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.SopImportBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaSopImportBatchRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SopImportServiceTest {

    @Mock JpaSopImportBatchRepository batchRepository;
    @Mock JpaRecipeRepository recipeRepository;
    @Mock JpaSkuRepository skuRepository;
    @Mock JpaInventoryItemRepository inventoryItemRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;
    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() { if (securityMock != null) securityMock.close(); }

    private SopImportService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new SopImportService(batchRepository, recipeRepository, skuRepository,
            inventoryItemRepository, new SopCsvParser(), enforcer);
    }

    private void setupActor(Long merchantId, Long storeId) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "admin", "A001", "STORE_MANAGER",
            merchantId, storeId, Set.of(storeId), Set.of("RECIPE_MANAGE"));
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(actor);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    @Test
    void importCsv_allValid_createsRecipesAndCompletes() {
        setupActor(100L, 10L);
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(batchRepository.save(any())).thenAnswer(inv -> {
            SopImportBatchEntity e = inv.getArgument(0);
            try { var f = SopImportBatchEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(e, 1L); } catch (Exception ex) { throw new RuntimeException(ex); }
            return e;
        });
        when(skuRepository.existsById(10L)).thenReturn(true);
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "Beef", "kg", BigDecimal.ZERO);
        try { var f = InventoryItemEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(item, 100L); } catch (Exception e) { throw new RuntimeException(e); }
        when(inventoryItemRepository.findByStoreIdAndItemCode(10L, "BEEF-001")).thenReturn(Optional.of(item));

        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n10,BEEF-001,0.2000,kg,,Beef\n";
        SopImportBatchDto result = buildService().importCsv(10L, "sop.csv", csv);

        assertThat(result.batchStatus()).isEqualTo("COMPLETED");
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEqualTo(0);
        verify(recipeRepository).deleteBySkuId(10L);
        verify(recipeRepository).save(any());
    }

    @Test
    void importCsv_skuNotFound_reportsErrorRow() {
        setupActor(100L, 10L);
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(skuRepository.existsById(10L)).thenReturn(false);

        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n10,BEEF-001,0.2000,kg,,\n";
        SopImportBatchDto result = buildService().importCsv(10L, "sop.csv", csv);

        assertThat(result.batchStatus()).isEqualTo("COMPLETED");
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.successRows()).isEqualTo(0);
    }

    @Test
    void importCsv_itemNotInStore_reportsErrorRow() {
        setupActor(100L, 10L);
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(skuRepository.existsById(10L)).thenReturn(true);
        when(inventoryItemRepository.findByStoreIdAndItemCode(10L, "BEEF-001")).thenReturn(Optional.empty());

        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n10,BEEF-001,0.2000,kg,,\n";
        SopImportBatchDto result = buildService().importCsv(10L, "sop.csv", csv);

        assertThat(result.errorRows()).isEqualTo(1);
    }

    @Test
    void importCsv_malformedCsv_reportsParseErrors() {
        setupActor(100L, 10L);
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\nabc,BEEF-001,0.2000,kg,,\n";
        SopImportBatchDto result = buildService().importCsv(10L, "bad.csv", csv);

        assertThat(result.errorRows()).isGreaterThan(0);
    }
}
