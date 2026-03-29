package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.inventory.application.dto.ConfirmedItemInput;
import com.developer.pos.v2.inventory.application.dto.PurchaseInvoiceDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.*;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseInvoiceService implements UseCase {

    private final JpaPurchaseInvoiceRepository invoiceRepository;
    private final JpaPurchaseInvoiceItemRepository invoiceItemRepository;
    private final JpaInventoryItemRepository inventoryItemRepository;
    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryMovementRepository movementRepository;
    private final StoreAccessEnforcer enforcer;

    public PurchaseInvoiceService(JpaPurchaseInvoiceRepository invoiceRepository,
                                   JpaPurchaseInvoiceItemRepository invoiceItemRepository,
                                   JpaInventoryItemRepository inventoryItemRepository,
                                   JpaInventoryBatchRepository batchRepository,
                                   JpaInventoryMovementRepository movementRepository,
                                   StoreAccessEnforcer enforcer) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.enforcer = enforcer;
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
    public PurchaseInvoiceDto triggerOcrScan(Long storeId, Long invoiceId, String imageAssetId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("PURCHASE_CREATE");
        PurchaseInvoiceEntity invoice = loadInvoiceForStore(storeId, invoiceId);
        invoice.startOcrScan(imageAssetId);
        return toDto(invoiceRepository.save(invoice));
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

        // Eager state guard — must be PROCESSING before touching any line items
        if (!"PROCESSING".equals(invoice.getOcrStatus())) {
            throw new IllegalStateException(
                "Invoice " + invoiceId + " is not in OCR PROCESSING state, current: " + invoice.getOcrStatus());
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
        invoice.completeOcr(totalCents);
        return toDto(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public List<PurchaseInvoiceDto> listInvoices(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW"); // read operation — lowest inventory permission
        return invoiceRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
            .stream().map(this::toDto).toList();
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
