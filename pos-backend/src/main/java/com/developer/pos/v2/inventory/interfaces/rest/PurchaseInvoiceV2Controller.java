package com.developer.pos.v2.inventory.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.inventory.application.dto.PurchaseInvoiceDto;
import com.developer.pos.v2.inventory.application.service.PurchaseInvoiceService;
import com.developer.pos.v2.inventory.interfaces.rest.request.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class PurchaseInvoiceV2Controller implements V2Api {

    private final PurchaseInvoiceService purchaseInvoiceService;

    public PurchaseInvoiceV2Controller(PurchaseInvoiceService purchaseInvoiceService) {
        this.purchaseInvoiceService = purchaseInvoiceService;
    }

    /** Create a new purchase invoice (送货单). */
    @PostMapping("/stores/{storeId}/invoices")
    public ApiResponse<PurchaseInvoiceDto> createInvoice(
            @PathVariable Long storeId,
            @Valid @RequestBody CreatePurchaseInvoiceRequest request) {
        return ApiResponse.success(purchaseInvoiceService.createInvoice(
            storeId, request.invoiceNo(), request.supplierId(),
            request.supplierName(), request.invoiceDate()));
    }

    /** List purchase invoices for a store. */
    @GetMapping("/stores/{storeId}/invoices")
    public ApiResponse<List<PurchaseInvoiceDto>> listInvoices(@PathVariable Long storeId) {
        return ApiResponse.success(purchaseInvoiceService.listInvoices(storeId));
    }

    /** Trigger OCR scan on a purchase invoice image. */
    @PostMapping("/stores/{storeId}/invoices/{invoiceId}/ocr-scan")
    public ApiResponse<PurchaseInvoiceDto> triggerOcrScan(
            @PathVariable Long storeId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody TriggerOcrRequest request) {
        return ApiResponse.success(purchaseInvoiceService.triggerOcrScan(
            storeId, invoiceId, request.imageAssetId()));
    }

    /** Confirm OCR result → create batches + movements + update stock. */
    @PostMapping("/stores/{storeId}/invoices/{invoiceId}/ocr-confirm")
    public ApiResponse<PurchaseInvoiceDto> confirmOcr(
            @PathVariable Long storeId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody ConfirmOcrRequest request) {
        return ApiResponse.success(purchaseInvoiceService.confirmOcr(
            storeId, invoiceId, request.items()));
    }
}
