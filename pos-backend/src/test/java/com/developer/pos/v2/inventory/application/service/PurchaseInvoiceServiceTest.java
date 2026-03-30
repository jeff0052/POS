package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.*;
import com.developer.pos.v2.inventory.application.port.OcrClient;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.*;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.*;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseInvoiceServiceTest {

    @Mock JpaPurchaseInvoiceRepository invoiceRepository;
    @Mock JpaPurchaseInvoiceItemRepository invoiceItemRepository;
    @Mock JpaInventoryItemRepository inventoryItemRepository;
    @Mock JpaInventoryBatchRepository batchRepository;
    @Mock JpaInventoryMovementRepository movementRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;
    @Mock OcrClient ocrClient;
    @Mock OcrAutoMatchService autoMatchService;
    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() {
        if (securityMock != null) securityMock.close();
    }

    private PurchaseInvoiceService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new PurchaseInvoiceService(new com.fasterxml.jackson.databind.ObjectMapper(),
            invoiceRepository, invoiceItemRepository,
            inventoryItemRepository, batchRepository, movementRepository, enforcer,
            ocrClient, autoMatchService);
    }

    private void setupActor(Long merchantId, Long storeId, Set<String> permissions) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "manager", "M001", "STORE_MANAGER",
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

    private PurchaseInvoiceEntity buildInvoice(Long id, Long storeId, String ocrStatus) {
        PurchaseInvoiceEntity inv = new PurchaseInvoiceEntity(
            storeId, "INV-001", null, "Supplier A", LocalDate.now());
        try {
            var f = PurchaseInvoiceEntity.class.getDeclaredField("id");
            f.setAccessible(true); f.set(inv, id);
            if (ocrStatus != null) {
                var s = PurchaseInvoiceEntity.class.getDeclaredField("ocrStatus");
                s.setAccessible(true); s.set(inv, ocrStatus);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return inv;
    }

    private PurchaseInvoiceEntity buildInvoiceWithOcrResult(Long id, Long storeId, String ocrStatus, String ocrRawResult) {
        PurchaseInvoiceEntity inv = buildInvoice(id, storeId, ocrStatus);
        try {
            var f = PurchaseInvoiceEntity.class.getDeclaredField("ocrRawResult");
            f.setAccessible(true); f.set(inv, ocrRawResult);
        } catch (Exception e) { throw new RuntimeException(e); }
        return inv;
    }

    // ─── createInvoice tests ───────────────────────────────────────────────

    @Test
    void createInvoice_happy_createsPendingInvoice() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(invoiceRepository.existsByStoreIdAndInvoiceNo(10L, "INV-001")).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            PurchaseInvoiceEntity e = inv.getArgument(0);
            try {
                var f = PurchaseInvoiceEntity.class.getDeclaredField("id");
                f.setAccessible(true); f.set(e, 1L);
            } catch (Exception ex) { throw new RuntimeException(ex); }
            return e;
        });

        PurchaseInvoiceDto result = buildService().createInvoice(
            10L, "INV-001", null, "Supplier A", LocalDate.now());

        assertThat(result.invoiceStatus()).isEqualTo("PENDING");
        assertThat(result.invoiceNo()).isEqualTo("INV-001");
        verify(invoiceRepository).save(any());
    }

    @Test
    void createInvoice_duplicateNo_throwsIllegalArgument() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(invoiceRepository.existsByStoreIdAndInvoiceNo(10L, "INV-DUP")).thenReturn(true);

        assertThatThrownBy(() -> buildService().createInvoice(
            10L, "INV-DUP", null, "X", LocalDate.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("INV-DUP");
    }

    @Test
    void triggerOcr_happy_callsOcrClientAndReturnsResult() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, null);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<OcrLineItem> ocrLines = List.of(
            new OcrLineItem("牛腩", BigDecimal.valueOf(10), "kg", 5000L, 50000L));
        OcrRawResult rawResult = new OcrRawResult("Supplier A", "2026-03-30", 50000L,
            new BigDecimal("0.95"), ocrLines);
        when(ocrClient.recognize("asset-abc-123")).thenReturn(rawResult);

        List<OcrMatchedItem> matched = List.of(
            new OcrMatchedItem("牛腩", 20L, "牛腩", new BigDecimal("0.95"),
                BigDecimal.valueOf(10), "kg", 5000L, 50000L));
        when(autoMatchService.match(eq(10L), eq(ocrLines))).thenReturn(matched);

        OcrResultDto result = buildService().triggerOcrScan(10L, 1L, "asset-abc-123");

        assertThat(result.supplierName()).isEqualTo("Supplier A");
        assertThat(result.avgConfidence()).isEqualByComparingTo(new BigDecimal("0.95"));
        assertThat(result.items()).hasSize(1);
        verify(ocrClient).recognize("asset-abc-123");
        verify(autoMatchService).match(eq(10L), eq(ocrLines));
    }

    @Test
    void triggerOcr_alreadyProcessing_throwsIllegalState() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "PROCESSING"); // already processing
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> buildService().triggerOcrScan(10L, 1L, "asset-xyz"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listInvoices_returnsStoreInvoices() {
        setupActor(100L, 10L, Set.of("INVENTORY_VIEW"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, null);
        when(invoiceRepository.findByStoreIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(invoice));

        List<PurchaseInvoiceDto> result = buildService().listInvoices(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).invoiceNo()).isEqualTo("INV-001");
    }

    // ─── confirmOcr tests ─────────────────────────────────────────────────

    @Test
    void confirmOcr_singleItem_createsBatchMovementAndUpdatesStock() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.valueOf(5));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmedItemInput> items = List.of(
            new ConfirmedItemInput(20L, BigDecimal.valueOf(10), "kg", 5000L, null));

        buildService().confirmOcr(10L, 1L, items);

        // stock incremented
        assertThat(item.getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(10));
        // invoice confirmed
        assertThat(invoice.getInvoiceStatus()).isEqualTo("CONFIRMED");
        // S5: ocrReviewedBy should be set (userId = 1L from setupActor)
        assertThat(invoice.getOcrReviewedBy()).isEqualTo(1L);
        verify(batchRepository).save(any());
        verify(movementRepository).save(any());
    }

    @Test
    void confirmOcr_multipleItems_updatesEachItemStock() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InventoryItemEntity beef = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.ZERO);
        InventoryItemEntity chili = new InventoryItemEntity(10L, "CHILI-001", "辣椒", "kg", BigDecimal.ZERO);
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(beef));
        when(inventoryItemRepository.findById(21L)).thenReturn(Optional.of(chili));
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmedItemInput> items = List.of(
            new ConfirmedItemInput(20L, BigDecimal.valueOf(10), "kg", 5000L, null),
            new ConfirmedItemInput(21L, BigDecimal.valueOf(2), "kg", 3000L, LocalDate.now().plusDays(30)));

        buildService().confirmOcr(10L, 1L, items);

        assertThat(beef.getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(chili.getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(2));
        verify(batchRepository, times(2)).save(any());
        verify(movementRepository, times(2)).save(any());
    }

    @Test
    void confirmOcr_invoiceNotInProcessingState_throwsIllegalState() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, null); // not PROCESSING
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L,
            List.of(new ConfirmedItemInput(20L, BigDecimal.TEN, "kg", 5000L, null))))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmOcr_inventoryItemNotFound_throwsIllegalArgument() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(inventoryItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L,
            List.of(new ConfirmedItemInput(99L, BigDecimal.TEN, "kg", 5000L, null))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }

    @Test
    void confirmOcr_wrongStore_throwsSecurityException() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(999L); // different merchant
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L, List.of()))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void confirmOcr_requiresCompletedOcrStatus() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "PROCESSING"); // not COMPLETED
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L,
            List.of(new ConfirmedItemInput(20L, BigDecimal.TEN, "kg", 5000L, null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not completed");
    }

    @Test
    void triggerOcrScan_callsOcrClientAndReturnsMatchedResult() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, null);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<OcrLineItem> ocrLines = List.of(
            new OcrLineItem("辣椒", BigDecimal.valueOf(5), "kg", 3000L, 15000L));
        OcrRawResult rawResult = new OcrRawResult("Supplier B", "2026-03-30", 15000L,
            new BigDecimal("0.88"), ocrLines);
        when(ocrClient.recognize("asset-xyz")).thenReturn(rawResult);

        List<OcrMatchedItem> matched = List.of(
            new OcrMatchedItem("辣椒", 21L, "辣椒", new BigDecimal("0.88"),
                BigDecimal.valueOf(5), "kg", 3000L, 15000L));
        when(autoMatchService.match(eq(10L), eq(ocrLines))).thenReturn(matched);

        OcrResultDto result = buildService().triggerOcrScan(10L, 1L, "asset-xyz");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).matchedItemName()).isEqualTo("辣椒");
        assertThat(result.totalAmountCents()).isEqualTo(15000L);
        verify(ocrClient).recognize("asset-xyz");
    }

    // ─── I2: Duplicate inventoryItemId test ─────────────────────────────

    @Test
    void confirmOcr_duplicateInventoryItemId_createsBothBatches() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InventoryItemEntity item = new InventoryItemEntity(10L, "BEEF-001", "牛腩", "kg", BigDecimal.ZERO);
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmedItemInput> items = List.of(
            new ConfirmedItemInput(20L, BigDecimal.valueOf(5), "kg", 5000L, null),
            new ConfirmedItemInput(20L, BigDecimal.valueOf(3), "kg", 6000L, null));

        buildService().confirmOcr(10L, 1L, items);

        // Both batches created for the same inventoryItemId
        verify(batchRepository, times(2)).save(any());
        verify(movementRepository, times(2)).save(any());
        // Stock should reflect both additions
        assertThat(item.getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }

    // ─── I3: OCR retry after FAILED ──────────────────────────────────────

    @Test
    void triggerOcr_retryAfterFailed_succeeds() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "FAILED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<OcrLineItem> ocrLines = List.of(
            new OcrLineItem("牛腩", BigDecimal.valueOf(10), "kg", 5000L, 50000L));
        OcrRawResult rawResult = new OcrRawResult("Supplier A", "2026-03-30", 50000L,
            new BigDecimal("0.95"), ocrLines);
        when(ocrClient.recognize("asset-retry")).thenReturn(rawResult);
        when(autoMatchService.match(eq(10L), eq(ocrLines))).thenReturn(List.of());

        OcrResultDto result = buildService().triggerOcrScan(10L, 1L, "asset-retry");

        assertThat(result.supplierName()).isEqualTo("Supplier A");
        verify(ocrClient).recognize("asset-retry");
    }

    // ─── I13: OCR client exception test ──────────────────────────────────

    @Test
    void triggerOcr_ocrClientThrows_failsInvoiceAndThrows() {
        setupActor(100L, 10L, Set.of("PURCHASE_CREATE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, null);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ocrClient.recognize("asset-fail")).thenThrow(new RuntimeException("OCR service down"));

        assertThatThrownBy(() -> buildService().triggerOcrScan(10L, 1L, "asset-fail"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OCR recognition failed");

        assertThat(invoice.getOcrStatus()).isEqualTo("FAILED");
    }

    // ─── I14: Empty confirmedItems test ──────────────────────────────────

    @Test
    void confirmOcr_emptyList_throwsIllegalArgument() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be empty");
    }

    // ─── I15: Cross-store inventoryItem test ─────────────────────────────

    @Test
    void confirmOcr_crossStoreInventoryItem_throwsSecurityException() {
        setupActor(100L, 10L, Set.of("PURCHASE_APPROVE"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        PurchaseInvoiceEntity invoice = buildInvoice(1L, 10L, "COMPLETED");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Item belongs to store 99, not store 10
        InventoryItemEntity crossStoreItem = new InventoryItemEntity(99L, "BEEF-001", "牛腩", "kg", BigDecimal.ZERO);
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(crossStoreItem));

        assertThatThrownBy(() -> buildService().confirmOcr(10L, 1L,
            List.of(new ConfirmedItemInput(20L, BigDecimal.TEN, "kg", 5000L, null))))
            .isInstanceOf(SecurityException.class);
    }

    // ─── getOcrResult tests ──────────────────────────────────────────────

    @Test
    void getOcrResult_returnsMatchedItems() throws Exception {
        setupActor(100L, 10L, Set.of("INVENTORY_VIEW"));
        StoreEntity store = buildStore(100L);
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));

        ObjectMapper mapper = new ObjectMapper();
        List<OcrLineItem> ocrLines = List.of(
            new OcrLineItem("牛腩", BigDecimal.valueOf(10), "kg", 5000L, 50000L));
        OcrRawResult rawResult = new OcrRawResult("Supplier A", "2026-03-30", 50000L,
            new BigDecimal("0.95"), ocrLines);
        String rawJson = mapper.writeValueAsString(rawResult);

        PurchaseInvoiceEntity invoice = buildInvoiceWithOcrResult(1L, 10L, "COMPLETED", rawJson);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        List<OcrMatchedItem> matched = List.of(
            new OcrMatchedItem("牛腩", 20L, "牛腩", new BigDecimal("0.95"),
                BigDecimal.valueOf(10), "kg", 5000L, 50000L));
        when(autoMatchService.match(eq(10L), any())).thenReturn(matched);

        OcrResultDto result = buildService().getOcrResult(10L, 1L);

        assertThat(result.supplierName()).isEqualTo("Supplier A");
        assertThat(result.totalAmountCents()).isEqualTo(50000L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).matchedItemName()).isEqualTo("牛腩");
        verify(autoMatchService).match(eq(10L), any());
    }
}
