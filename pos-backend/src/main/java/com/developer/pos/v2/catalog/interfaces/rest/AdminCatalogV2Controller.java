package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogCategoryDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogProductDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogSkuDto;
import com.developer.pos.v2.catalog.application.service.AdminCatalogReadService;
import com.developer.pos.v2.catalog.application.service.AdminCatalogWriteService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpsertCatalogCategoryRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpsertCatalogProductRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/catalog")
public class AdminCatalogV2Controller implements V2Api {

    private final AdminCatalogReadService adminCatalogReadService;
    private final AdminCatalogWriteService adminCatalogWriteService;

    public AdminCatalogV2Controller(
            AdminCatalogReadService adminCatalogReadService,
            AdminCatalogWriteService adminCatalogWriteService
    ) {
        this.adminCatalogReadService = adminCatalogReadService;
        this.adminCatalogWriteService = adminCatalogWriteService;
    }

    @GetMapping("/products")
    public ApiResponse<List<AdminCatalogProductDto>> getProducts(@RequestParam String storeCode) {
        return ApiResponse.success(adminCatalogReadService.getProducts(storeCode));
    }

    @GetMapping("/skus")
    public ApiResponse<List<AdminCatalogSkuDto>> getSkus(
            @RequestParam String storeCode,
            @RequestParam Long productId
    ) {
        return ApiResponse.success(adminCatalogReadService.getSkus(storeCode, productId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<AdminCatalogCategoryDto>> getCategories(@RequestParam String storeCode) {
        return ApiResponse.success(adminCatalogReadService.getCategories(storeCode));
    }

    @PostMapping("/categories")
    public ApiResponse<AdminCatalogCategoryDto> createCategory(@Valid @RequestBody UpsertCatalogCategoryRequest request) {
        return ApiResponse.success(adminCatalogWriteService.upsertCategory(
                null,
                request.storeCode(),
                request.categoryCode(),
                request.name(),
                request.enabled(),
                request.sortOrder()
        ));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<AdminCatalogCategoryDto> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpsertCatalogCategoryRequest request
    ) {
        return ApiResponse.success(adminCatalogWriteService.upsertCategory(
                categoryId,
                request.storeCode(),
                request.categoryCode(),
                request.name(),
                request.enabled(),
                request.sortOrder()
        ));
    }

    @PostMapping("/products")
    public ApiResponse<AdminCatalogProductDto> createProduct(@Valid @RequestBody UpsertCatalogProductRequest request) {
        return ApiResponse.success(upsertProduct(null, request));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<AdminCatalogProductDto> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpsertCatalogProductRequest request
    ) {
        return ApiResponse.success(upsertProduct(productId, request));
    }

    private AdminCatalogProductDto upsertProduct(Long productId, UpsertCatalogProductRequest request) {
        return adminCatalogWriteService.upsertProduct(
                productId,
                request.storeCode(),
                request.categoryId(),
                request.productCode(),
                request.name(),
                request.status(),
                request.skus().stream()
                        .map(item -> new AdminCatalogWriteService.UpsertSkuCommand(
                                item.skuId(),
                                item.skuCode(),
                                item.name(),
                                item.priceCents(),
                                item.status(),
                                item.available()
                        ))
                        .toList(),
                request.attributeGroups() == null ? List.of() : request.attributeGroups().stream()
                        .map(item -> new AdminCatalogWriteService.UpsertAttributeGroupCommand(
                                item.code(),
                                item.name(),
                                item.selectionMode(),
                                item.required(),
                                item.minSelect(),
                                item.maxSelect(),
                                item.values() == null ? List.of() : item.values().stream()
                                        .map(value -> new AdminCatalogWriteService.UpsertAttributeValueCommand(
                                                value.code(),
                                                value.label(),
                                                value.priceDeltaCents(),
                                                value.defaultSelected(),
                                                value.kitchenLabel()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                request.modifierGroups() == null ? List.of() : request.modifierGroups().stream()
                        .map(item -> new AdminCatalogWriteService.UpsertModifierGroupCommand(
                                item.code(),
                                item.name(),
                                item.freeQuantity(),
                                item.minSelect(),
                                item.maxSelect(),
                                item.options() == null ? List.of() : item.options().stream()
                                        .map(option -> new AdminCatalogWriteService.UpsertModifierOptionCommand(
                                                option.code(),
                                                option.label(),
                                                option.priceDeltaCents(),
                                                option.defaultSelected(),
                                                option.kitchenLabel()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                request.comboSlots() == null ? List.of() : request.comboSlots().stream()
                        .map(item -> new AdminCatalogWriteService.UpsertComboSlotCommand(
                                item.code(),
                                item.name(),
                                item.minSelect(),
                                item.maxSelect(),
                                item.allowedSkuCodes() == null ? List.of() : item.allowedSkuCodes()
                        ))
                        .toList()
        );
    }
}
