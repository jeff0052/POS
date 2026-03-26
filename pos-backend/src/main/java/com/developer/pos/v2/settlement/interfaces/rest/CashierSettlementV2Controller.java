package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.dto.SettlementPreviewDto;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class CashierSettlementV2Controller implements V2Api {

    private final CashierSettlementApplicationService cashierSettlementApplicationService;

    public CashierSettlementV2Controller(CashierSettlementApplicationService cashierSettlementApplicationService) {
        this.cashierSettlementApplicationService = cashierSettlementApplicationService;
    }

    @GetMapping("/active-table-orders/{activeOrderId}/settlement-preview")
    public ApiResponse<SettlementPreviewDto> getSettlementPreview(@PathVariable String activeOrderId) {
        return ApiResponse.success(cashierSettlementApplicationService.getSettlementPreview(activeOrderId));
    }

    // Old /cashier-settlement/{activeOrderId}/collect endpoint REMOVED.
    // All settlement collection now goes through:
    //   1. /api/v2/stores/{storeId}/tables/{tableId}/payment/collect (table-based, with amount validation)
    //   2. /api/v2/internal/payment-callback (payment service callback)
}
