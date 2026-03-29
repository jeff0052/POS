package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.dto.StackingCollectResultDto;
import com.developer.pos.v2.settlement.application.dto.StackingPreviewDto;
import com.developer.pos.v2.settlement.application.dto.PaymentRetryResultDto;
import com.developer.pos.v2.settlement.application.service.PaymentRetryService;
import com.developer.pos.v2.settlement.application.service.PaymentStackingService;
import com.developer.pos.v2.settlement.interfaces.rest.request.CollectStackingRequest;
import com.developer.pos.v2.settlement.interfaces.rest.request.SwitchMethodRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2")
public class PaymentStackingV2Controller implements V2Api {

    private final PaymentStackingService stackingService;
    private final PaymentRetryService retryService;

    public PaymentStackingV2Controller(PaymentStackingService stackingService, PaymentRetryService retryService) {
        this.stackingService = stackingService;
        this.retryService = retryService;
    }

    @PostMapping("/stores/{storeId}/tables/{tableId}/payment/preview-stacking")
    public ApiResponse<StackingPreviewDto> previewStacking(
            @PathVariable Long storeId, @PathVariable Long tableId) {
        return ApiResponse.success(stackingService.previewStacking(storeId, tableId));
    }

    @PostMapping("/stores/{storeId}/tables/{tableId}/payment/collect-stacking")
    public ApiResponse<StackingCollectResultDto> collectStacking(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @RequestBody CollectStackingRequest req) {
        var choices = new PaymentStackingService.StackingChoices(
                req.usePoints(), req.couponId(), req.couponLockVersion(),
                req.useCashBalance(), req.externalPaymentMethod());
        return ApiResponse.success(stackingService.collectStacking(storeId, tableId, choices));
    }

    @PostMapping("/stores/{storeId}/tables/{tableId}/payment/{settlementId}/confirm-stacking")
    public ApiResponse<Void> confirmStacking(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @PathVariable Long settlementId) {
        stackingService.confirmStacking(settlementId);
        return ApiResponse.<Void>success(null);
    }

    @PostMapping("/stores/{storeId}/tables/{tableId}/payment/{settlementId}/release-stacking")
    public ApiResponse<Void> releaseStacking(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @PathVariable Long settlementId,
            @RequestParam(defaultValue = "MANUAL_RELEASE") String reason) {
        stackingService.releaseStacking(settlementId, reason);
        return ApiResponse.<Void>success(null);
    }

    @PostMapping("/stores/{storeId}/tables/{tableId}/payment/switch-method")
    public ApiResponse<PaymentRetryResultDto> switchMethod(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @RequestBody SwitchMethodRequest req) {
        return ApiResponse.success(retryService.switchMethod(storeId, tableId, req.paymentAttemptId(), req.newPaymentScheme()));
    }
}
