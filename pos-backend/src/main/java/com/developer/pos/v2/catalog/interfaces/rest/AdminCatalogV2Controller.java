package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogCategoryDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogProductDto;
import com.developer.pos.v2.catalog.application.service.AdminCatalogReadService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/catalog")
public class AdminCatalogV2Controller implements V2Api {

    private final AdminCatalogReadService adminCatalogReadService;

    public AdminCatalogV2Controller(AdminCatalogReadService adminCatalogReadService) {
        this.adminCatalogReadService = adminCatalogReadService;
    }

    @GetMapping("/products")
    public ApiResponse<List<AdminCatalogProductDto>> getProducts(@RequestParam String storeCode) {
        return ApiResponse.success(adminCatalogReadService.getProducts(storeCode));
    }

    @GetMapping("/categories")
    public ApiResponse<List<AdminCatalogCategoryDto>> getCategories(@RequestParam String storeCode) {
        return ApiResponse.success(adminCatalogReadService.getCategories(storeCode));
    }
}
