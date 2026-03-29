package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.InventoryItemDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock JpaInventoryItemRepository itemRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;
    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() {
        if (securityMock != null) securityMock.close();
    }

    private InventoryItemService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new InventoryItemService(itemRepository, enforcer);
    }

    private void setupActor(Long merchantId, Long storeId, Set<String> permissions) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "staff", "S001", "INVENTORY_CLERK",
            merchantId, storeId, Set.of(storeId), permissions);
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(actor);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    private StoreEntity buildStore(Long merchantId) {
        StoreEntity store = mock(StoreEntity.class);
        lenient().when(store.getMerchantId()).thenReturn(merchantId);
        return store;
    }

    @Test
    void createItem_happy_persistsAndReturnsDto() {
        setupActor(100L, 10L, Set.of("INVENTORY_MANAGE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(itemRepository.existsByStoreIdAndItemCode(10L, "BEEF-001")).thenReturn(false);
        when(itemRepository.save(any())).thenAnswer(inv -> {
            InventoryItemEntity e = inv.getArgument(0);
            try {
                var f = InventoryItemEntity.class.getDeclaredField("id");
                f.setAccessible(true); f.set(e, 1L);
            } catch (Exception ex) { throw new RuntimeException(ex); }
            return e;
        });

        InventoryItemDto result = buildService().createItem(10L, "BEEF-001", "牛腩", "kg", BigDecimal.valueOf(5));

        assertThat(result.itemCode()).isEqualTo("BEEF-001");
        assertThat(result.itemName()).isEqualTo("牛腩");
        assertThat(result.currentStock()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(itemRepository).save(any());
    }

    @Test
    void createItem_duplicateCode_throwsIllegalArgument() {
        setupActor(100L, 10L, Set.of("INVENTORY_MANAGE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(itemRepository.existsByStoreIdAndItemCode(10L, "BEEF-001")).thenReturn(true);

        assertThatThrownBy(() -> buildService().createItem(10L, "BEEF-001", "牛腩", "kg", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BEEF-001");
    }

    @Test
    void createItem_wrongStore_throwsSecurityException() {
        setupActor(100L, 10L, Set.of("INVENTORY_MANAGE"));
        StoreEntity store = buildStore(999L); // different merchant
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> buildService().createItem(10L, "X001", "Item", "kg", null))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void listItems_returnsOnlyActiveItems() {
        setupActor(100L, 10L, Set.of("INVENTORY_VIEW"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.valueOf(5));
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(10L, "ACTIVE"))
            .thenReturn(List.of(item));

        List<InventoryItemDto> result = buildService().listItems(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemCode()).isEqualTo("BEEF-001");
    }

    @Test
    void updateItem_updatesNameAndSafetyStock() {
        setupActor(100L, 10L, Set.of("INVENTORY_MANAGE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.valueOf(5));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItemDto result = buildService().updateItem(10L, 1L, "牛腩（精选）", null, BigDecimal.valueOf(8), null);

        assertThat(result.itemName()).isEqualTo("牛腩（精选）");
        assertThat(result.safetyStock()).isEqualByComparingTo(BigDecimal.valueOf(8));
        verify(itemRepository).save(item);
    }

    @Test
    void deactivateItem_setsItemStatusInactive() {
        setupActor(100L, 10L, Set.of("INVENTORY_MANAGE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.ZERO);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        buildService().deactivateItem(10L, 1L);

        assertThat(item.getItemStatus()).isEqualTo("INACTIVE");
        verify(itemRepository).save(item);
    }
}
