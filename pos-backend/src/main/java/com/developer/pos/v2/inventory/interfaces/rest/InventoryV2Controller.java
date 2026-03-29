package com.developer.pos.v2.inventory.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.inventory.application.dto.InventoryItemDto;
import com.developer.pos.v2.inventory.application.service.InventoryItemService;
import com.developer.pos.v2.inventory.interfaces.rest.request.CreateInventoryItemRequest;
import com.developer.pos.v2.inventory.interfaces.rest.request.UpdateInventoryItemRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class InventoryV2Controller implements V2Api {

    private final InventoryItemService inventoryItemService;

    public InventoryV2Controller(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    /** Create a new inventory item (ingredient/material) for a store. */
    @PostMapping("/stores/{storeId}/inventory-items")
    public ApiResponse<InventoryItemDto> createItem(
            @PathVariable Long storeId,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        return ApiResponse.success(inventoryItemService.createItem(
            storeId, request.itemCode(), request.itemName(),
            request.unit(), request.safetyStock()));
    }

    /** List active inventory items for a store. */
    @GetMapping("/stores/{storeId}/inventory-items")
    public ApiResponse<List<InventoryItemDto>> listItems(@PathVariable Long storeId) {
        return ApiResponse.success(inventoryItemService.listItems(storeId));
    }

    /** Get a single inventory item. */
    @GetMapping("/stores/{storeId}/inventory-items/{itemId}")
    public ApiResponse<InventoryItemDto> getItem(
            @PathVariable Long storeId,
            @PathVariable Long itemId) {
        return ApiResponse.success(inventoryItemService.getItem(storeId, itemId));
    }

    /** Update inventory item metadata (name, category, safetyStock, defaultSupplierId). */
    @PutMapping("/stores/{storeId}/inventory-items/{itemId}")
    public ApiResponse<InventoryItemDto> updateItem(
            @PathVariable Long storeId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        return ApiResponse.success(inventoryItemService.updateItem(
            storeId, itemId, request.itemName(), request.category(),
            request.safetyStock(), request.defaultSupplierId()));
    }

    /** Deactivate (soft-delete) an inventory item. */
    @DeleteMapping("/stores/{storeId}/inventory-items/{itemId}")
    public ApiResponse<InventoryItemDto> deactivateItem(
            @PathVariable Long storeId,
            @PathVariable Long itemId) {
        return ApiResponse.success(inventoryItemService.deactivateItem(storeId, itemId));
    }

    /** List inventory items that are below their safety stock threshold. */
    @GetMapping("/stores/{storeId}/inventory-items/low-stock")
    public ApiResponse<List<InventoryItemDto>> listLowStockItems(@PathVariable Long storeId) {
        return ApiResponse.success(inventoryItemService.listLowStockItems(storeId));
    }
}
