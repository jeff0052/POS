package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetBillDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetPricingServiceTest {

    @Mock JpaTableSessionRepository sessionRepo;
    @Mock JpaBuffetPackageRepository packageRepo;
    @Mock JpaSubmittedOrderRepository submittedOrderRepo;
    @Mock JpaStoreLookupRepository storeRepo;
    @InjectMocks BuffetPricingService service;

    private MockedStatic<SecurityContextHolder> securityMock;

    void setupActor(Long merchantId, Set<String> permissions, Set<Long> storeIds) {
        AuthenticatedActor actor = new AuthenticatedActor(
                1L, "test", "T001", "CASHIER",
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

    // ─── Helpers ─────────────────────────────────────────────────────────

    private void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BuffetPackageEntity buildPackage(long priceCents, Long childPriceCents,
                                              int durationMinutes, int graceMinutes,
                                              int maxOvertime, long feePerMinute) {
        BuffetPackageEntity pkg = new BuffetPackageEntity(
                10L, "PKG001", "Lunch Buffet", "desc",
                priceCents, childPriceCents, null,
                durationMinutes, 10,
                feePerMinute, graceMinutes, maxOvertime,
                "ACTIVE", "[]", "[]", 1, null, 1L
        );
        setId(pkg, 42L);
        return pkg;
    }

    private TableSessionEntity buildSession(int guestCount, int childCount,
                                             OffsetDateTime startedAt) {
        TableSessionEntity s = new TableSessionEntity();
        s.setStoreId(10L);
        s.setDiningMode("BUFFET");
        s.setGuestCount(guestCount);
        s.setChildCount(childCount);
        s.setBuffetPackageId(42L);
        s.setBuffetStartedAt(startedAt);
        setId(s, 100L);
        return s;
    }

    private SubmittedOrderItemEntity buildItem(boolean buffetIncluded,
                                                long surchargeCents,
                                                long lineTotalCents,
                                                int quantity) {
        SubmittedOrderItemEntity item = new SubmittedOrderItemEntity();
        item.setSkuId(1L);
        item.setSkuNameSnapshot("Item");
        item.setSkuCodeSnapshot("SKU001");
        item.setUnitPriceSnapshotCents(lineTotalCents / Math.max(1, quantity));
        item.setQuantity(quantity);
        item.setLineTotalCents(lineTotalCents);
        item.setBuffetIncluded(buffetIncluded);
        item.setBuffetSurchargeCents(surchargeCents);
        return item;
    }

    private SubmittedOrderEntity buildOrder(List<SubmittedOrderItemEntity> items) {
        SubmittedOrderEntity order = new SubmittedOrderEntity();
        order.setTableSessionId(100L);
        order.setMerchantId(5L);
        order.setStoreId(10L);
        order.setTableId(1L);
        order.setSubmittedOrderId("SO-001");
        order.setOrderNo("ORD-001");
        order.setSourceOrderType("TABLE");
        order.setFulfillmentStatus("FULFILLED");
        order.setSettlementStatus("UNSETTLED");
        order.setOriginalAmountCents(0L);
        order.setMemberDiscountCents(0L);
        order.setPromotionDiscountCents(0L);
        order.setPayableAmountCents(0L);
        items.forEach(order::addItem);
        return order;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 1: Adults only, no extras, no overtime
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_adultsOnly_noExtras() {
        // 4 adults × 16800 = 67200
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 0, OffsetDateTime.now().minusMinutes(30));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // duration=90min, 30min elapsed → no overtime
        BuffetPackageEntity pkg = buildPackage(16800L, null, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of());

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(67200L);
        assertThat(result.surchargeTotal()).isZero();
        assertThat(result.extraTotal()).isZero();
        assertThat(result.overtimeFeeCents()).isZero();
        assertThat(result.grandTotal()).isEqualTo(67200L);
        assertThat(result.guestCount()).isEqualTo(4);
        assertThat(result.childCount()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 2: Adults and children
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_adultsAndChildren() {
        // 4 adults × 16800 + 2 children × 9800 = 67200 + 19600 = 86800
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 2, OffsetDateTime.now().minusMinutes(30));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        BuffetPackageEntity pkg = buildPackage(16800L, 9800L, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of());

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(86800L);
        assertThat(result.surchargeTotal()).isZero();
        assertThat(result.extraTotal()).isZero();
        assertThat(result.overtimeFeeCents()).isZero();
        assertThat(result.grandTotal()).isEqualTo(86800L);
        assertThat(result.guestCount()).isEqualTo(4);
        assertThat(result.childCount()).isEqualTo(2);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 3: With surcharge items (buffetIncluded=true, surchargeCents > 0)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_withSurchargeItems() {
        // headFee: 4 × 16800 = 67200
        // surcharge: qty=2, surchargeCents=2800 → 5600
        // grand total: 67200 + 5600 = 72800
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 0, OffsetDateTime.now().minusMinutes(30));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        BuffetPackageEntity pkg = buildPackage(16800L, null, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        SubmittedOrderItemEntity surchargeItem = buildItem(true, 2800L, 5600L, 2);
        SubmittedOrderEntity order = buildOrder(List.of(surchargeItem));
        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of(order));

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(67200L);
        assertThat(result.surchargeTotal()).isEqualTo(5600L);
        assertThat(result.extraTotal()).isZero();
        assertThat(result.overtimeFeeCents()).isZero();
        assertThat(result.grandTotal()).isEqualTo(72800L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 4: With extra items (buffetIncluded=false)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_withExtraItems() {
        // headFee: 4 × 16800 = 67200
        // extra: lineTotalCents=3500
        // grand total: 67200 + 3500 = 70700
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 0, OffsetDateTime.now().minusMinutes(30));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        BuffetPackageEntity pkg = buildPackage(16800L, null, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        SubmittedOrderItemEntity extraItem = buildItem(false, 0L, 3500L, 1);
        SubmittedOrderEntity order = buildOrder(List.of(extraItem));
        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of(order));

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(67200L);
        assertThat(result.surchargeTotal()).isZero();
        assertThat(result.extraTotal()).isEqualTo(3500L);
        assertThat(result.overtimeFeeCents()).isZero();
        assertThat(result.grandTotal()).isEqualTo(70700L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 5: Overtime with grace period
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_overtimeWithGrace() {
        // 100min actual, 90min duration, 5min grace → billable = 100-90-5 = 5min × 200cents = 1000
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 0, OffsetDateTime.now().minusMinutes(100));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // duration=90, grace=5, max=60, fee=200 per minute
        BuffetPackageEntity pkg = buildPackage(16800L, null, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of());

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(67200L);
        assertThat(result.surchargeTotal()).isZero();
        assertThat(result.extraTotal()).isZero();
        // overtimeMinutes = 100 - 90 = 10, billable = 10 - 5 = 5 × 200 = 1000
        assertThat(result.overtimeFeeCents()).isEqualTo(1000L);
        assertThat(result.grandTotal()).isEqualTo(68200L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test 6: Overtime capped at maxOvertimeMinutes
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void calculate_overtimeCapped() {
        // 200min actual, 90min duration, 5min grace, max=60 → billable=60min × 200 = 12000
        setupActor(5L, Set.of("BUFFET_START"), Set.of(10L));

        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(5L);
        when(storeRepo.findById(10L)).thenReturn(Optional.of(store));

        TableSessionEntity session = buildSession(4, 0, OffsetDateTime.now().minusMinutes(200));
        when(sessionRepo.findById(100L)).thenReturn(Optional.of(session));

        // duration=90, grace=5, max=60, fee=200 per minute
        BuffetPackageEntity pkg = buildPackage(16800L, null, 90, 5, 60, 200L);
        when(packageRepo.findById(42L)).thenReturn(Optional.of(pkg));

        when(submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(100L)).thenReturn(List.of());

        BuffetBillDto result = service.calculateBuffetTotal(10L, 100L);

        assertThat(result.headFeeCents()).isEqualTo(67200L);
        assertThat(result.surchargeTotal()).isZero();
        assertThat(result.extraTotal()).isZero();
        // overtimeMinutes = 200-90=110, billable = 110-5=105, capped at 60 → 60 × 200 = 12000
        assertThat(result.overtimeFeeCents()).isEqualTo(12000L);
        assertThat(result.grandTotal()).isEqualTo(79200L);
    }
}
