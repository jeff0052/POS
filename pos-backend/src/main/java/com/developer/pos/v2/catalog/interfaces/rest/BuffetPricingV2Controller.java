package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.BuffetBillDto;
import com.developer.pos.v2.catalog.application.service.BuffetPricingService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/buffet-pricing")
public class BuffetPricingV2Controller implements V2Api {

    private final BuffetPricingService pricingService;

    public BuffetPricingV2Controller(BuffetPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/calculate")
    public ApiResponse<BuffetBillDto> calculateBuffetTotal(
            @PathVariable Long storeId,
            @RequestParam Long sessionId) {
        return ApiResponse.success(pricingService.calculateBuffetTotal(storeId, sessionId));
    }
}
