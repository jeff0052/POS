package com.developer.pos.v2.settlement.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.settlement.application.dto.VibeCashWebhookResultDto;
import com.developer.pos.v2.settlement.application.service.VibeCashPaymentApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/payments/vibecash")
public class VibeCashWebhookV2Controller implements V2Api {

    private final VibeCashPaymentApplicationService vibeCashPaymentApplicationService;

    public VibeCashWebhookV2Controller(VibeCashPaymentApplicationService vibeCashPaymentApplicationService) {
        this.vibeCashPaymentApplicationService = vibeCashPaymentApplicationService;
    }

    @PostMapping("/webhook")
    public ApiResponse<VibeCashWebhookResultDto> handleWebhook(
            @RequestHeader(value = "VibeCash-Signature", required = false) String signature,
            @RequestBody JsonNode payload
    ) {
        return ApiResponse.success(vibeCashPaymentApplicationService.handleWebhook(signature, payload));
    }
}
