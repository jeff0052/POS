package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.order.application.dto.OrderStageTransitionDto;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.application.dto.SettlementPreviewDto;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import com.developer.pos.v2.settlement.interfaces.rest.request.CollectCashierSettlementRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/tables/{tableId}/payment")
public class TableSettlementV2Controller implements V2Api {

    private final CashierSettlementApplicationService cashierSettlementApplicationService;

    public TableSettlementV2Controller(CashierSettlementApplicationService cashierSettlementApplicationService) {
        this.cashierSettlementApplicationService = cashierSettlementApplicationService;
    }

    @GetMapping("/preview")
    public ApiResponse<SettlementPreviewDto> getPreview(@PathVariable Long storeId, @PathVariable Long tableId) {
        return ApiResponse.success(cashierSettlementApplicationService.getTableSettlementPreview(storeId, tableId));
    }

    @PostMapping
    public ApiResponse<OrderStageTransitionDto> moveToPayment(@PathVariable Long storeId, @PathVariable Long tableId) {
        return ApiResponse.success(cashierSettlementApplicationService.moveTableToPaymentPending(storeId, tableId));
    }

    @PostMapping("/collect")
    public ApiResponse<CashierSettlementResultDto> collect(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @Valid @RequestBody CollectCashierSettlementRequest request
    ) {
        return ApiResponse.success(
                cashierSettlementApplicationService.collectForTable(
                        storeId,
                        tableId,
                        new CollectCashierSettlementCommand(
                                "table-" + tableId,
                                request.cashierId(),
                                request.paymentMethod(),
                                request.collectedAmountCents()
                        )
                )
        );
    }
}
