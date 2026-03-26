package com.developer.pos.order.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.dto.QrCurrentOrderResponse;
import com.developer.pos.order.dto.QrOrderSubmitRequest;
import com.developer.pos.order.dto.QrOrderSubmitResponse;
import com.developer.pos.order.dto.QrOrderSettleRequest;
import com.developer.pos.order.dto.QrOrderUpdateRequest;
import com.developer.pos.order.service.ActiveTableOrderService;
import com.developer.pos.order.service.OrderService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final ActiveTableOrderService activeTableOrderService;

    public OrderController(OrderService orderService, ActiveTableOrderService activeTableOrderService) {
        this.orderService = orderService;
        this.activeTableOrderService = activeTableOrderService;
    }

    @GetMapping
    public ApiResponse<OrderListResponse> list(@RequestParam(defaultValue = "1001") Long storeId) {
        return ApiResponse.success(orderService.list(storeId));
    }

    @PostMapping("/qr-submit")
    public ApiResponse<QrOrderSubmitResponse> submitQrOrder(@RequestBody QrOrderSubmitRequest request) {
        return ApiResponse.success(activeTableOrderService.submitQrOrder(request));
    }

    @GetMapping("/qr-current")
    public ApiResponse<QrCurrentOrderResponse> currentQrOrder(
        @RequestParam String storeCode,
        @RequestParam String tableCode
    ) {
        return ApiResponse.success(activeTableOrderService.getCurrentQrOrder(storeCode, tableCode));
    }

    @PutMapping("/qr-current")
    public ApiResponse<QrCurrentOrderResponse> updateCurrentQrOrder(@RequestBody QrOrderUpdateRequest request) {
        return ApiResponse.success(activeTableOrderService.updateCurrentQrOrder(request));
    }

    @DeleteMapping("/qr-current")
    public ApiResponse<Void> clearCurrentQrOrder(
        @RequestParam String storeCode,
        @RequestParam String tableCode
    ) {
        activeTableOrderService.clearCurrentQrOrder(storeCode, tableCode);
        return ApiResponse.success(null);
    }

    @PostMapping("/qr-settle")
    public ApiResponse<Boolean> settleCurrentQrOrder(@RequestBody QrOrderSettleRequest request) {
        activeTableOrderService.settleCurrentQrOrder(request);
        return ApiResponse.success(true);
    }
}
