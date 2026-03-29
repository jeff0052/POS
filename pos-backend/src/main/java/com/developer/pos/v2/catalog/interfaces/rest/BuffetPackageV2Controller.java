package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageDto;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageItemDto;
import com.developer.pos.v2.catalog.application.service.BuffetPackageService;
import com.developer.pos.v2.catalog.interfaces.rest.request.BindSkuRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.CreateBuffetPackageRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpdateBindingRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpdateBuffetPackageRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/buffet-packages")
public class BuffetPackageV2Controller implements V2Api {

    private final BuffetPackageService buffetPackageService;

    public BuffetPackageV2Controller(BuffetPackageService buffetPackageService) {
        this.buffetPackageService = buffetPackageService;
    }

    // ─── Package CRUD ────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<List<BuffetPackageDto>> listPackages(@PathVariable Long storeId) {
        return ApiResponse.success(buffetPackageService.listPackages(storeId));
    }

    @GetMapping("/{packageId}")
    public ApiResponse<BuffetPackageDto> getPackage(
            @PathVariable Long storeId,
            @PathVariable Long packageId
    ) {
        return ApiResponse.success(buffetPackageService.getPackage(storeId, packageId));
    }

    @PostMapping
    public ApiResponse<BuffetPackageDto> createPackage(
            @PathVariable Long storeId,
            @Valid @RequestBody CreateBuffetPackageRequest request
    ) {
        return ApiResponse.success(buffetPackageService.createPackage(storeId, request));
    }

    @PutMapping("/{packageId}")
    public ApiResponse<BuffetPackageDto> updatePackage(
            @PathVariable Long storeId,
            @PathVariable Long packageId,
            @Valid @RequestBody UpdateBuffetPackageRequest request
    ) {
        return ApiResponse.success(buffetPackageService.updatePackage(storeId, packageId, request));
    }

    @DeleteMapping("/{packageId}")
    public ApiResponse<Void> deletePackage(
            @PathVariable Long storeId,
            @PathVariable Long packageId
    ) {
        buffetPackageService.deletePackage(storeId, packageId);
        return ApiResponse.success(null);
    }

    // ─── SKU Binding ─────────────────────────────────────────────────────

    @GetMapping("/{packageId}/items")
    public ApiResponse<List<BuffetPackageItemDto>> listPackageItems(
            @PathVariable Long storeId,
            @PathVariable Long packageId
    ) {
        return ApiResponse.success(buffetPackageService.listPackageItems(storeId, packageId));
    }

    @PostMapping("/{packageId}/items")
    public ApiResponse<BuffetPackageItemDto> bindSku(
            @PathVariable Long storeId,
            @PathVariable Long packageId,
            @Valid @RequestBody BindSkuRequest request
    ) {
        return ApiResponse.success(buffetPackageService.bindSku(storeId, packageId, request));
    }

    @PutMapping("/{packageId}/items/{itemId}")
    public ApiResponse<BuffetPackageItemDto> updatePackageItem(
            @PathVariable Long storeId,
            @PathVariable Long packageId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateBindingRequest request
    ) {
        return ApiResponse.success(buffetPackageService.updatePackageItem(storeId, packageId, itemId, request));
    }

    @DeleteMapping("/{packageId}/items/{itemId}")
    public ApiResponse<Void> unbindSku(
            @PathVariable Long storeId,
            @PathVariable Long packageId,
            @PathVariable Long itemId
    ) {
        buffetPackageService.unbindSku(storeId, packageId, itemId);
        return ApiResponse.success(null);
    }
}
