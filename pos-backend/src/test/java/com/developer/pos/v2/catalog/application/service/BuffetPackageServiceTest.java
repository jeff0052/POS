package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageDto;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageItemDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.catalog.interfaces.rest.request.BindSkuRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.CreateBuffetPackageRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpdateBuffetPackageRequest;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetPackageServiceTest {

    @Mock JpaBuffetPackageRepository packageRepo;
    @Mock JpaBuffetPackageItemRepository itemRepo;
    @Mock JpaStoreLookupRepository storeRepo;
    @Mock JpaSkuRepository skuRepo;
    @Mock ImageUploadService imageUploadService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks BuffetPackageService service;

    private MockedStatic<SecurityContextHolder> securityMock;

    void setupActor(Long merchantId, Set<String> permissions, Set<Long> storeIds) {
        AuthenticatedActor actor = new AuthenticatedActor(
                1L, "test", "T001", "STORE_MANAGER",
                merchantId, 10L, storeIds, permissions);
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(actor);
        when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    @AfterEach
    void tearDown() {
        if (securityMock != null) {
            securityMock.close();
        }
    }

    // ─── Test 1: createPackage success ───────────────────────────────────

    @Test
    void createPackage_success() throws Exception {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        BuffetPackageEntity saved = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", "All you can eat",
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        when(packageRepo.save(any())).thenReturn(saved);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of());

        CreateBuffetPackageRequest req = new CreateBuffetPackageRequest(
                "PKG001", "Lunch Buffet", "All you can eat",
                19900L, null, null,
                90, 10, 100L, 5, 30,
                null, null, 1, null
        );

        BuffetPackageDto result = service.createPackage(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.packageCode()).isEqualTo("PKG001");
        verify(packageRepo).save(any(BuffetPackageEntity.class));
    }

    // ─── Test 2: createPackage no permission throws ───────────────────────

    @Test
    void createPackage_noPermission_throws() {
        setupActor(5L, Set.of("MENU_VIEW"), Set.of(10L));

        CreateBuffetPackageRequest req = new CreateBuffetPackageRequest(
                "PKG001", "Lunch Buffet", null,
                19900L, null, null,
                90, 10, 100L, 5, 30,
                null, null, 1, null
        );

        assertThatThrownBy(() -> service.createPackage(10L, req))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("BUFFET_MANAGE");
    }

    // ─── Test 3: createPackage wrong store throws ─────────────────────────

    @Test
    void createPackage_wrongStore_throws() {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(99L)); // store 10 not in accessible set

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        CreateBuffetPackageRequest req = new CreateBuffetPackageRequest(
                "PKG001", "Lunch Buffet", null,
                19900L, null, null,
                90, 10, 100L, 5, 30,
                null, null, 1, null
        );

        assertThatThrownBy(() -> service.createPackage(10L, req))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("access to store");
    }

    // ─── Test 4: listPackages MENU_VIEW sufficient ────────────────────────

    @Test
    void listPackages_menuViewSufficient() throws Exception {
        setupActor(0L, Set.of("MENU_VIEW"), Set.of());

        when(packageRepo.findByStoreIdAndPackageStatusOrderBySortOrderAsc(10L, "ACTIVE"))
                .thenReturn(List.of());

        List<BuffetPackageDto> result = service.listPackages(10L);

        assertThat(result).isEmpty();
    }

    // ─── Test 5: deletePackage soft deletes ───────────────────────────────

    @Test
    void deletePackage_softDelete() throws Exception {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity existing = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", null,
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        when(packageRepo.findById(42L)).thenReturn(Optional.of(existing));
        when(packageRepo.save(any())).thenReturn(existing);

        service.deletePackage(10L, 42L);

        verify(packageRepo).save(any(BuffetPackageEntity.class));
        assertThat(existing.getPackageStatus()).isEqualTo("INACTIVE");
    }

    // ─── Test 6: bindSku success ──────────────────────────────────────────

    @Test
    void bindSku_success() throws Exception {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", null,
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));
        when(itemRepo.findByPackageIdAndSkuId(42L, 100L)).thenReturn(Optional.empty());

        BuffetPackageItemEntity savedItem = new BuffetPackageItemEntity(42L, 100L, "INCLUDED", 0L, null, 1);
        when(itemRepo.save(any())).thenReturn(savedItem);

        SkuEntity sku = mock(SkuEntity.class);
        when(sku.getSkuCode()).thenReturn("SKU-100");
        when(sku.getSkuName()).thenReturn("Test Dish");
        when(skuRepo.findById(100L)).thenReturn(Optional.of(sku));

        BindSkuRequest req = new BindSkuRequest(100L, "INCLUDED", 0L, null, 1);
        BuffetPackageItemDto result = service.bindSku(10L, 42L, req);

        assertThat(result).isNotNull();
        assertThat(result.skuId()).isEqualTo(100L);
        assertThat(result.inclusionType()).isEqualTo("INCLUDED");
        verify(itemRepo).save(any(BuffetPackageItemEntity.class));
    }

    // ─── Test 7: bindSku duplicate throws ────────────────────────────────

    @Test
    void bindSku_duplicate_throws() {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", null,
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        BuffetPackageItemEntity existing = new BuffetPackageItemEntity(42L, 100L, "INCLUDED", 0L, null, 1);
        when(itemRepo.findByPackageIdAndSkuId(42L, 100L)).thenReturn(Optional.of(existing));

        BindSkuRequest req = new BindSkuRequest(100L, "INCLUDED", 0L, null, 1);
        assertThatThrownBy(() -> service.bindSku(10L, 42L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already bound");
    }

    // ─── Test 8: updatePackage success ───────────────────────────────────

    @Test
    void updatePackage_success() throws Exception {
        setupActor(5L, Set.of("BUFFET_MANAGE"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        BuffetPackageEntity existing = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", null,
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        when(packageRepo.findById(42L)).thenReturn(Optional.of(existing));
        when(packageRepo.save(any())).thenReturn(existing);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of());

        UpdateBuffetPackageRequest req = new UpdateBuffetPackageRequest(
                "PKG001-UPDATED", "Dinner Buffet", "Updated description",
                24900L, null, null,
                120, 15, 200L, 10, 60,
                null, null, 2, null
        );

        BuffetPackageDto result = service.updatePackage(10L, 42L, req);

        assertThat(result).isNotNull();
        verify(packageRepo).save(any(BuffetPackageEntity.class));
    }
}
