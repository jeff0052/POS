package com.developer.pos.order.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.dto.QrCurrentOrderResponse;
import com.developer.pos.order.dto.QrOrderSubmitRequest;
import com.developer.pos.order.dto.QrOrderSubmitResponse;
import com.developer.pos.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<OrderListResponse> list() {
        return ApiResponse.success(orderService.list());
    }

    @PostMapping("/qr-submit")
    public ApiResponse<QrOrderSubmitResponse> submitQrOrder(@RequestBody QrOrderSubmitRequest request) {
        return ApiResponse.success(orderService.submitQrOrder(request));
    }

    @GetMapping("/qr-current")
    public ApiResponse<QrCurrentOrderResponse> currentQrOrder(
        @RequestParam String storeCode,
        @RequestParam String tableCode
    ) {
        return ApiResponse.success(orderService.getCurrentQrOrder(storeCode, tableCode));
    }
}
