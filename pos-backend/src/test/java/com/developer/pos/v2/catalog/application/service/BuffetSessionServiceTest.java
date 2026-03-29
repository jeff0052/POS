package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetStatusDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
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

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetSessionServiceTest {

    @Mock JpaTableSessionRepository sessionRepo;
    @Mock JpaBuffetPackageRepository packageRepo;
    @Mock JpaBuffetPackageItemRepository itemRepo;
    @Mock JpaStoreLookupRepository storeRepo;
    @InjectMocks BuffetSessionService service;

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

    // ─── Helper: build a standard package ────────────────────────────────

    private BuffetPackageEntity buildPackage(Long storeId, String status) {
        BuffetPackageEntity pkg = new BuffetPackageEntity(
                storeId, "PKG001", "Lunch Buffet", "All you can eat",
                19900L, null, null, 90, 10, 100L, 5, 30,
                status, "[]", "[]", 1, null, 1L
        );
        setId(pkg, 42L);
        return pkg;
    }

    private void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TableSessionEntity buildSession(String diningMode) {
        TableSessionEntity s = new TableSessionEntity();
        s.setStoreId(10L);
        s.setTableId(1L);
        s.setSessionStatus("OPEN");
        s.setDiningMode(diningMode);
        s.setGuestCount(4);
        s.setChildCount(1);
        return s;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Start Buffet — 4 cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void startBuffet_success() {
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = buildPackage(10L, "ACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        TableSessionEntity session = buildSession("A_LA_CARTE");
        when(sessionRepo.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(10L, 1L, "OPEN"))
                .thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        BuffetStatusDto result = service.startBuffet(10L, 1L,
                new BuffetSessionService.StartBuffetRequest(42L, 4, 1));

        assertThat(result).isNotNull();
        assertThat(result.buffetStatus()).isEqualTo("ACTIVE");
        assertThat(result.guestCount()).isEqualTo(4);
        assertThat(result.childCount()).isEqualTo(1);
        assertThat(result.packageName()).isEqualTo("Lunch Buffet");
        assertThat(session.getDiningMode()).isEqualTo("BUFFET");
        assertThat(session.getBuffetPackageId()).isNotNull();
        assertThat(session.getBuffetStartedAt()).isNotNull();
        assertThat(session.getBuffetEndsAt()).isNotNull();
        verify(sessionRepo).save(any(TableSessionEntity.class));
    }

    @Test
    void startBuffet_packageNotActive_throws() {
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = buildPackage(10L, "INACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> service.startBuffet(10L, 1L,
                new BuffetSessionService.StartBuffetRequest(42L, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void startBuffet_alreadyBuffet_throws() {
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = buildPackage(10L, "ACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        TableSessionEntity session = buildSession("BUFFET");
        when(sessionRepo.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(10L, 1L, "OPEN"))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.startBuffet(10L, 1L,
                new BuffetSessionService.StartBuffetRequest(42L, 4, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void startBuffet_packageWrongStore_throws() {
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        BuffetPackageEntity pkg = buildPackage(99L, "ACTIVE"); // Wrong store
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> service.startBuffet(10L, 1L,
                new BuffetSessionService.StartBuffetRequest(42L, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Get Status — 3 cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void getStatus_active() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetStartedAt(OffsetDateTime.now().minusMinutes(30));
        session.setBuffetEndsAt(OffsetDateTime.now().plusMinutes(60));
        session.setBuffetPackageId(42L);
        session.setBuffetStatus("ACTIVE");
        when(sessionRepo.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(10L, 1L, "OPEN"))
                .thenReturn(Optional.of(session));

        BuffetPackageEntity pkg = buildPackage(10L, "ACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        BuffetStatusDto result = service.getBuffetStatus(10L, 1L);

        assertThat(result.buffetStatus()).isEqualTo("ACTIVE");
        // remainingMinutes should be around 59-60
        assertThat(result.remainingMinutes()).isBetween(58L, 61L);
        assertThat(result.overtimeMinutes()).isZero();
        assertThat(result.forceCheckout()).isFalse();
    }

    @Test
    void getStatus_warning() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetStartedAt(OffsetDateTime.now().minusMinutes(85));
        session.setBuffetEndsAt(OffsetDateTime.now().plusMinutes(5));
        session.setBuffetPackageId(42L);
        session.setBuffetStatus("ACTIVE");
        when(sessionRepo.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(10L, 1L, "OPEN"))
                .thenReturn(Optional.of(session));

        BuffetPackageEntity pkg = buildPackage(10L, "ACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        BuffetStatusDto result = service.getBuffetStatus(10L, 1L);

        assertThat(result.buffetStatus()).isEqualTo("WARNING");
        assertThat(result.remainingMinutes()).isBetween(4L, 6L);
    }

    @Test
    void getStatus_overtime() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetStartedAt(OffsetDateTime.now().minusMinutes(110));
        session.setBuffetEndsAt(OffsetDateTime.now().minusMinutes(20));
        session.setBuffetPackageId(42L);
        session.setBuffetStatus("ACTIVE");
        when(sessionRepo.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(10L, 1L, "OPEN"))
                .thenReturn(Optional.of(session));

        // Package: graceMinutes=5, maxOvertimeMinutes=30, feePerMinute=100 cents
        BuffetPackageEntity pkg = buildPackage(10L, "ACTIVE");
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        BuffetStatusDto result = service.getBuffetStatus(10L, 1L);

        assertThat(result.buffetStatus()).isEqualTo("OVERTIME");
        // overtimeMinutes should be around 19-21
        assertThat(result.overtimeMinutes()).isBetween(19L, 21L);
        // chargeable = max(0, ~20 - 5) = 15, capped at 30 => 15 * 100 = 1500
        assertThat(result.overtimeFeeCents()).isBetween(1400L, 1600L);
        assertThat(result.forceCheckout()).isFalse(); // 20 < 30
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Validate Order — 5 cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void validateOrder_allIncluded_pass() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetPackageId(42L);
        session.setGuestCount(2);
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        BuffetPackageItemEntity item1 = new BuffetPackageItemEntity(42L, 200L, "INCLUDED", 0L, null, 1);
        BuffetPackageItemEntity item2 = new BuffetPackageItemEntity(42L, 201L, "SURCHARGE", 500L, null, 2);
        when(itemRepo.findByPackageIdAndSkuId(42L, 200L)).thenReturn(Optional.of(item1));
        when(itemRepo.findByPackageIdAndSkuId(42L, 201L)).thenReturn(Optional.of(item2));

        var result = service.validateBuffetOrder(100L, List.of(
                new BuffetSessionService.ValidateOrderItemRequest(200L, 1),
                new BuffetSessionService.ValidateOrderItemRequest(201L, 1)
        ));

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).buffetIncluded()).isTrue();
        assertThat(result.items().get(0).rejected()).isFalse();
        assertThat(result.items().get(0).surchargeCents()).isZero();
        assertThat(result.items().get(1).buffetIncluded()).isTrue();
        assertThat(result.items().get(1).surchargeCents()).isEqualTo(500L);
    }

    @Test
    void validateOrder_itemNotInPackage_markedAsExtra() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetPackageId(42L);
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        when(itemRepo.findByPackageIdAndSkuId(42L, 999L)).thenReturn(Optional.empty());

        var result = service.validateBuffetOrder(100L, List.of(
                new BuffetSessionService.ValidateOrderItemRequest(999L, 1)
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).buffetIncluded()).isFalse();
        assertThat(result.items().get(0).rejected()).isFalse();
        assertThat(result.items().get(0).buffetPackageId()).isNull();
    }

    @Test
    void validateOrder_excludedItem_rejected() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetPackageId(42L);
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        BuffetPackageItemEntity excluded = new BuffetPackageItemEntity(42L, 300L, "EXCLUDED", 0L, null, 1);
        when(itemRepo.findByPackageIdAndSkuId(42L, 300L)).thenReturn(Optional.of(excluded));

        var result = service.validateBuffetOrder(100L, List.of(
                new BuffetSessionService.ValidateOrderItemRequest(300L, 1)
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).rejected()).isTrue();
        assertThat(result.items().get(0).rejectionReason()).isEqualTo("excluded from package");
    }

    @Test
    void validateOrder_limitAtBoundary_pass() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetPackageId(42L);
        session.setGuestCount(3);
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // maxQtyPerPerson=2, guestCount=3 => limit=6
        BuffetPackageItemEntity item = new BuffetPackageItemEntity(42L, 400L, "INCLUDED", 0L, 2, 1);
        when(itemRepo.findByPackageIdAndSkuId(42L, 400L)).thenReturn(Optional.of(item));

        var result = service.validateBuffetOrder(100L, List.of(
                new BuffetSessionService.ValidateOrderItemRequest(400L, 6) // exactly at limit
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).rejected()).isFalse();
        assertThat(result.items().get(0).buffetIncluded()).isTrue();
    }

    @Test
    void validateOrder_limitExceeded_rejected() {
        TableSessionEntity session = buildSession("BUFFET");
        session.setBuffetPackageId(42L);
        session.setGuestCount(3);
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // maxQtyPerPerson=2, guestCount=3 => limit=6
        BuffetPackageItemEntity item = new BuffetPackageItemEntity(42L, 400L, "INCLUDED", 0L, 2, 1);
        when(itemRepo.findByPackageIdAndSkuId(42L, 400L)).thenReturn(Optional.of(item));

        var result = service.validateBuffetOrder(100L, List.of(
                new BuffetSessionService.ValidateOrderItemRequest(400L, 7) // exceeds limit
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).rejected()).isTrue();
        assertThat(result.items().get(0).rejectionReason()).contains("exceeds limit");
    }
}
