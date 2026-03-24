package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import com.developer.pos.v2.settlement.interfaces.rest.request.CollectCashierSettlementRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/cashier-settlement")
public class CashierSettlementV2Controller implements V2Api {

    private final CashierSettlementApplicationService cashierSettlementApplicationService;

    public CashierSettlementV2Controller(CashierSettlementApplicationService cashierSettlementApplicationService) {
        this.cashierSettlementApplicationService = cashierSettlementApplicationService;
    }

    @PostMapping("/{activeOrderId}/collect")
    public ApiResponse<CashierSettlementResultDto> collect(
            @PathVariable String activeOrderId,
            @Valid @RequestBody CollectCashierSettlementRequest request
    ) {
        return ApiResponse.success(
                cashierSettlementApplicationService.collect(
                        new CollectCashierSettlementCommand(
                                activeOrderId,
                                request.cashierId(),
                                request.paymentMethod(),
                                request.collectedAmountCents()
                        )
                )
        );
    }
}
