package com.developer.pos.v2.order.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.order.application.dto.MerchantAdminOrderDto;
import com.developer.pos.v2.order.application.service.MerchantOrderReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/orders")
public class MerchantOrderV2Controller implements V2Api {

    private final MerchantOrderReadService merchantOrderReadService;

    public MerchantOrderV2Controller(MerchantOrderReadService merchantOrderReadService) {
        this.merchantOrderReadService = merchantOrderReadService;
    }

    @GetMapping
    public ApiResponse<List<MerchantAdminOrderDto>> getOrders(@RequestParam Long storeId) {
        return ApiResponse.success(merchantOrderReadService.getOrders(storeId));
    }
}
