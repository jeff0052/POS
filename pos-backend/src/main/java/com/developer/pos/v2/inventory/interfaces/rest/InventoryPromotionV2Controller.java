package com.developer.pos.v2.inventory.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.application.service.InventoryPromotionApprovalService;
import com.developer.pos.v2.inventory.application.service.InventoryPromotionScanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class InventoryPromotionV2Controller implements V2Api {

    private final InventoryPromotionScanService scanService;
    private final InventoryPromotionApprovalService approvalService;

    public InventoryPromotionV2Controller(
            InventoryPromotionScanService scanService,
            InventoryPromotionApprovalService approvalService) {
        this.scanService = scanService;
        this.approvalService = approvalService;
    }

    @PostMapping("/stores/{storeId}/inventory-promotions/scan")
    public ApiResponse<List<InventoryDrivenPromotionDto>> triggerScan(@PathVariable Long storeId) {
        return ApiResponse.success(scanService.scanAll(storeId));
    }

    @GetMapping("/stores/{storeId}/inventory-promotions")
    public ApiResponse<List<InventoryDrivenPromotionDto>> listDrafts(
            @PathVariable Long storeId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(approvalService.listDrafts(storeId, status));
    }

    @PostMapping("/stores/{storeId}/inventory-promotions/{draftId}/approve")
    public ApiResponse<InventoryDrivenPromotionDto> approve(
            @PathVariable Long storeId,
            @PathVariable Long draftId) {
        return ApiResponse.success(approvalService.approveDraft(storeId, draftId));
    }

    @PostMapping("/stores/{storeId}/inventory-promotions/{draftId}/reject")
    public ApiResponse<InventoryDrivenPromotionDto> reject(
            @PathVariable Long storeId,
            @PathVariable Long draftId) {
        return ApiResponse.success(approvalService.rejectDraft(storeId, draftId));
    }
}
