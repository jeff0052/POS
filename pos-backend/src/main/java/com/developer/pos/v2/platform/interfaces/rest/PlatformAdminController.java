package com.developer.pos.v2.platform.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.platform.application.service.PlatformDashboardService;
import com.developer.pos.v2.platform.application.service.PlatformMerchantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/platform")
public class PlatformAdminController implements V2Api {

    private final PlatformDashboardService dashboardService;
    private final PlatformMerchantService merchantService;

    public PlatformAdminController(
            PlatformDashboardService dashboardService,
            PlatformMerchantService merchantService
    ) {
        this.dashboardService = dashboardService;
        this.merchantService = merchantService;
    }

    // ── Dashboard ──

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.success(dashboardService.getDashboard());
    }

    // ── Merchants ──

    @GetMapping("/merchants")
    public ApiResponse<List<Map<String, Object>>> listMerchants() {
        return ApiResponse.success(merchantService.listMerchants());
    }

    @PostMapping("/merchants")
    public ApiResponse<Void> createMerchant(@RequestBody CreateMerchantRequest request) {
        merchantService.createMerchant(request.merchantCode(), request.merchantName());
        return ApiResponse.success(null);
    }

    // ── Brands ──

    @GetMapping("/merchants/{merchantId}/brands")
    public ApiResponse<List<Map<String, Object>>> listBrands(@PathVariable Long merchantId) {
        return ApiResponse.success(merchantService.listBrands(merchantId));
    }

    @PostMapping("/merchants/{merchantId}/brands")
    public ApiResponse<Void> createBrand(@PathVariable Long merchantId, @RequestBody CreateBrandRequest request) {
        merchantService.createBrand(merchantId, request.brandCode(), request.brandName());
        return ApiResponse.success(null);
    }

    // ── Countries ──

    @GetMapping("/brands/{brandId}/countries")
    public ApiResponse<List<Map<String, Object>>> listCountries(@PathVariable Long brandId) {
        return ApiResponse.success(merchantService.listCountries(brandId));
    }

    @PostMapping("/brands/{brandId}/countries")
    public ApiResponse<Void> createCountry(@PathVariable Long brandId, @RequestBody CreateCountryRequest request) {
        merchantService.createCountry(brandId, request.countryCode(), request.countryName(), request.currencyCode(), request.timezone());
        return ApiResponse.success(null);
    }

    // ── Request DTOs ──

    public record CreateMerchantRequest(String merchantCode, String merchantName) {}
    public record CreateBrandRequest(String brandCode, String brandName) {}
    public record CreateCountryRequest(String countryCode, String countryName, String currencyCode, String timezone) {}
}
