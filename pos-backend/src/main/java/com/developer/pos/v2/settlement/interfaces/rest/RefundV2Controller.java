package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.command.ApproveRefundCommand;
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
        List<CreateRefundCommand.RefundItemCommand> items = request.refundItems() == null ? null :
                request.refundItems().stream()
                        .map(i -> new CreateRefundCommand.RefundItemCommand(i.itemId(), i.quantity()))
                        .toList();

        CreateRefundCommand command = new CreateRefundCommand(
                request.settlementId(),
                request.refundAmountCents(),
                request.refundType(),
                request.reason(),
                items
        );
        return ApiResponse.success(refundApplicationService.createRefund(command));
    }

    @PostMapping("/{refundNo}/approve")
    public ApiResponse<RefundRecordDto> approveRefund(
            @PathVariable String refundNo,
            @Valid @RequestBody ApproveRefundRequest request
    ) {
        ApproveRefundCommand command = new ApproveRefundCommand(refundNo, request.approved());
        return ApiResponse.success(refundApplicationService.approveRefund(command));
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
        String reason,
        List<RefundItemRequest> refundItems
    ) {}

    public record RefundItemRequest(
        @NotNull Long itemId,
        @Positive int quantity
    ) {}

    public record ApproveRefundRequest(
        boolean approved
    ) {}
}
