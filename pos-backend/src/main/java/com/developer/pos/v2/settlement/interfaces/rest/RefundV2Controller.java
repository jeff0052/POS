package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.application.service.RefundApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/refunds")
public class RefundV2Controller implements V2Api {

    private final RefundApplicationService refundApplicationService;

    public RefundV2Controller(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @PostMapping
    public ApiResponse<RefundRecordDto> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        CreateRefundCommand command = new CreateRefundCommand(
                request.settlementId(),
                request.refundAmountCents(),
                request.refundType(),
                request.refundReason(),
                request.operatedBy(),
                0L,
                null
        );
        return ApiResponse.success(refundApplicationService.createRefund(command));
    }

    @GetMapping("/{refundNo}")
    public ApiResponse<RefundRecordDto> getRefund(@PathVariable String refundNo) {
        return ApiResponse.success(refundApplicationService.getRefund(refundNo));
    }

    @GetMapping("/by-settlement/{settlementId}")
    public ApiResponse<List<RefundRecordDto>> getRefundsBySettlement(@PathVariable Long settlementId) {
        return ApiResponse.success(refundApplicationService.getRefundsBySettlement(settlementId));
    }

    @GetMapping
    public ApiResponse<Page<RefundRecordDto>> listRefunds(
            @RequestParam @Positive Long storeId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return ApiResponse.success(refundApplicationService.listRefunds(storeId, page, size));
    }

    public record CreateRefundRequest(
        @NotNull(message = "settlementId is required") Long settlementId,
        @PositiveOrZero(message = "refundAmountCents must be >= 0") long refundAmountCents,
        String refundType,
        String refundReason,
        Long operatedBy
    ) {}
}
