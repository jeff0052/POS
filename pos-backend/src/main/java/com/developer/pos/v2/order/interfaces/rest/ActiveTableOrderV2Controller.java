package com.developer.pos.v2.order.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.order.application.command.ReplaceActiveTableOrderItemsCommand;
import com.developer.pos.v2.order.application.dto.ActiveTableOrderDto;
import com.developer.pos.v2.order.application.dto.OrderStageTransitionDto;
import com.developer.pos.v2.order.application.dto.SubmittedOrderDto;
import com.developer.pos.v2.order.application.query.GetActiveTableOrderQuery;
import com.developer.pos.v2.order.application.service.ActiveTableOrderApplicationService;
import com.developer.pos.v2.order.interfaces.rest.request.ReplaceActiveTableOrderItemsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/tables/{tableId}/active-order")
public class ActiveTableOrderV2Controller implements V2Api {

    private final ActiveTableOrderApplicationService activeTableOrderApplicationService;

    public ActiveTableOrderV2Controller(ActiveTableOrderApplicationService activeTableOrderApplicationService) {
        this.activeTableOrderApplicationService = activeTableOrderApplicationService;
    }

    @GetMapping
    public ApiResponse<ActiveTableOrderDto> getActiveTableOrder(
            @PathVariable Long storeId,
            @PathVariable Long tableId
    ) {
        return ApiResponse.success(
                activeTableOrderApplicationService.getActiveTableOrder(
                        new GetActiveTableOrderQuery(storeId, tableId, "T" + tableId)
                )
        );
    }

    @GetMapping("/submitted-orders")
    public ApiResponse<java.util.List<SubmittedOrderDto>> getSubmittedOrders(
            @PathVariable Long storeId,
            @PathVariable Long tableId
    ) {
        return ApiResponse.success(activeTableOrderApplicationService.getSubmittedOrders(storeId, tableId));
    }

    @PutMapping("/items")
    public ApiResponse<ActiveTableOrderDto> replaceItems(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @Valid @RequestBody ReplaceActiveTableOrderItemsRequest request
    ) {
        ReplaceActiveTableOrderItemsCommand command = new ReplaceActiveTableOrderItemsCommand(
                storeId,
                tableId,
                "T" + tableId,
                request.orderSource(),
                request.memberId(),
                request.items().stream()
                        .map(item -> new ReplaceActiveTableOrderItemsCommand.ReplaceActiveTableOrderItemInput(
                                item.skuId(),
                                item.skuCode(),
                                item.skuName(),
                                item.quantity(),
                                item.unitPriceCents(),
                                item.remark()
                        ))
                        .toList()
        );

        return ApiResponse.success(activeTableOrderApplicationService.replaceItems(command));
    }

    @PostMapping("/{activeOrderId}/move-to-settlement")
    public ApiResponse<OrderStageTransitionDto> moveToSettlement(@PathVariable String activeOrderId) {
        return ApiResponse.success(activeTableOrderApplicationService.moveToSettlement(activeOrderId));
    }

    @PostMapping("/{activeOrderId}/submit-to-kitchen")
    public ApiResponse<OrderStageTransitionDto> submitToKitchen(@PathVariable String activeOrderId) {
        return ApiResponse.success(activeTableOrderApplicationService.submitToKitchen(activeOrderId));
    }

    @DeleteMapping("/{activeOrderId}/empty-draft")
    public ApiResponse<Boolean> deleteEmptyDraft(@PathVariable String activeOrderId) {
        activeTableOrderApplicationService.deleteEmptyDraft(activeOrderId);
        return ApiResponse.success(true);
    }
}
