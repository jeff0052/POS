package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.inventory.application.dto.*;
import com.developer.pos.v2.inventory.application.port.OcrClient;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.*;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseInvoiceService implements UseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JpaPurchaseInvoiceRepository invoiceRepository;
    private final JpaPurchaseInvoiceItemRepository invoiceItemRepository;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryMovementRepository movementRepository;
    private final StoreAccessEnforcer enforcer;
    private final OcrClient ocrClient;
    private final OcrAutoMatchService autoMatchService;

    public PurchaseInvoiceService(JpaPurchaseInvoiceRepository invoiceRepository,
                                   JpaPurchaseInvoiceItemRepository invoiceItemRepository,
                                   JpaInventoryItemRepository inventoryItemRepository,
                                   JpaInventoryBatchRepository batchRepository,
                                   JpaInventoryMovementRepository movementRepository,
                                   StoreAccessEnforcer enforcer,
                                   OcrClient ocrClient,
                                   OcrAutoMatchService autoMatchService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.enforcer = enforcer;
        this.ocrClient = ocrClient;
        this.autoMatchService = autoMatchService;
    }

    @Transactional
    public PurchaseInvoiceDto createInvoice(Long storeId, String invoiceNo, Long supplierId,
                                             String supplierName, LocalDate invoiceDate) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PURCHASE_CREATE");
        if (invoiceRepository.existsByStoreIdAndInvoiceNo(storeId, invoiceNo)) {
            throw new IllegalArgumentException("Invoice number already exists: " + invoiceNo);
        }
        PurchaseInvoiceEntity invoice = new PurchaseInvoiceEntity(
            storeId, invoiceNo, supplierId, supplierName, invoiceDate);
        return toDto(invoiceRepository.save(invoice));
    }

    @Transactional
    public OcrResultDto triggerOcrScan(Long storeId, Long invoiceId, String imageAssetId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PURCHASE_CREATE");
        PurchaseInvoiceEntity invoice = loadInvoiceForStore(storeId, invoiceId);
        invoice.startOcrScan(imageAssetId);

        // Call OCR provider and store raw result
        OcrRawResult ocrResult = ocrClient.recognize(imageAssetId);
        invoice.completeOcrScan(ocrResult.avgConfidence(), serializeJson(ocrResult));
        invoiceRepository.save(invoice);

        // Auto-match recognized items to inventory
        List<OcrMatchedItem> matched = autoMatchService.match(storeId, ocrResult.items());

        return new OcrResultDto(
            ocrResult.supplierName(), ocrResult.invoiceDate(),
            ocrResult.totalAmountCents(), ocrResult.avgConfidence(), matched);
    }

    /**
     * Confirms OCR result: creates invoice items, batches, movements, updates stock.
     * Called after employee verifies the OCR output.
     */
    @Transactional
    public PurchaseInvoiceDto confirmOcr(Long storeId, Long invoiceId,
                                          List<ConfirmedItemInput> confirmedItems) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PURCHASE_APPROVE");
        PurchaseInvoiceEntity invoice = loadInvoiceForStore(storeId, invoiceId);

        // Eager state guard — must be COMPLETED before touching any line items
        if (!"COMPLETED".equals(invoice.getOcrStatus())) {
            throw new IllegalStateException(
                "Invoice " + invoiceId + " OCR is not completed, current: " + invoice.getOcrStatus());
        }

        long totalCents = 0L;
        int seq = 1;

        for (ConfirmedItemInput confirmed : confirmedItems) {
            InventoryItemEntity inventoryItem = inventoryItemRepository.findById(confirmed.inventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Inventory item not found: " + confirmed.inventoryItemId()));
            if (!inventoryItem.getStoreId().equals(storeId)) {
                throw new SecurityException("Item " + confirmed.inventoryItemId()
                    + " does not belong to store " + storeId);
            }

            // 1. Create invoice line item
            PurchaseInvoiceItemEntity lineItem = new PurchaseInvoiceItemEntity(
                invoiceId, confirmed.inventoryItemId(),
                confirmed.quantity(), confirmed.unit(), confirmed.unitPriceCents());
            invoiceItemRepository.save(lineItem);
            totalCents += lineItem.getLineTotalCents();

            // 2. Create FIFO batch
            String batchNo = "BATCH-" + invoiceId + "-" + seq++;
            InventoryBatchEntity batch = new InventoryBatchEntity(
                storeId, confirmed.inventoryItemId(), batchNo,
                "PURCHASE", "INV-" + invoiceId,
                confirmed.quantity(), confirmed.unit(),
                confirmed.unitPriceCents(), confirmed.expiryDate());
            batch.setSupplierId(invoice.getSupplierId());
            InventoryBatchEntity savedBatch = batchRepository.save(batch);

            // 3. Update stock and last purchase price
            inventoryItem.addStock(confirmed.quantity());
            inventoryItem.setLastPurchasePriceCents(confirmed.unitPriceCents());
            InventoryItemEntity savedItem = inventoryItemRepository.save(inventoryItem);

            // 4. Record movement
            InventoryMovementEntity movement = new InventoryMovementEntity(
                storeId, confirmed.inventoryItemId(), savedBatch.getId(),
                "PURCHASE", confirmed.quantity(),
                confirmed.unitPriceCents(), savedItem.getCurrentStock(),
                "PURCHASE_INVOICE", "INV-" + invoiceId);
            movementRepository.save(movement);
        }

        // 5. Confirm invoice
        invoice.completeOcr(totalCents, AuthContext.current().userId());
        return toDto(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public List<PurchaseInvoiceDto> listInvoices(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW"); // read operation — lowest inventory permission
        return invoiceRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OcrResultDto getOcrResult(Long storeId, Long invoiceId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");
        PurchaseInvoiceEntity invoice = loadInvoiceForStore(storeId, invoiceId);
        if (invoice.getOcrRawResult() == null) {
            throw new IllegalStateException("Invoice " + invoiceId + " has no OCR result");
        }
        OcrRawResult raw = deserializeOcrResult(invoice.getOcrRawResult());
        List<OcrMatchedItem> matched = autoMatchService.match(storeId, raw.items());
        return new OcrResultDto(raw.supplierName(), raw.invoiceDate(),
            raw.totalAmountCents(), raw.avgConfidence(), matched);
    }

    private String serializeJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); }
        catch (Exception e) { throw new IllegalStateException("Failed to serialize OCR result", e); }
    }

    private OcrRawResult deserializeOcrResult(String json) {
        try { return MAPPER.readValue(json, OcrRawResult.class); }
        catch (Exception e) { throw new IllegalStateException("Failed to parse OCR result", e); }
    }

    private PurchaseInvoiceEntity loadInvoiceForStore(Long storeId, Long invoiceId) {
        PurchaseInvoiceEntity invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (!invoice.getStoreId().equals(storeId)) {
            throw new SecurityException("Invoice " + invoiceId + " does not belong to store " + storeId);
        }
        return invoice;
    }

    private PurchaseInvoiceDto toDto(PurchaseInvoiceEntity e) {
        return new PurchaseInvoiceDto(e.getId(), e.getStoreId(), e.getInvoiceNo(),
            e.getSupplierId(), e.getSupplierName(), e.getInvoiceDate(),
            e.getTotalAmountCents(), e.getInvoiceStatus(),
            e.getOcrStatus(), e.getScanImageUrl());
    }
}
