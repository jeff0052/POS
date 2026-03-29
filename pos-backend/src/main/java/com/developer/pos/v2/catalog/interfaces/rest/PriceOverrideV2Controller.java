package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.SkuPriceOverrideDto;
import com.developer.pos.v2.catalog.application.service.SkuPriceOverrideService;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpsertSkuPriceOverrideRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/price-overrides")
public class PriceOverrideV2Controller implements V2Api {

    private final SkuPriceOverrideService priceOverrideService;

    public PriceOverrideV2Controller(SkuPriceOverrideService priceOverrideService) {
        this.priceOverrideService = priceOverrideService;
    }

    @GetMapping("/sku/{skuId}")
    public ApiResponse<List<SkuPriceOverrideDto>> listOverrides(@PathVariable Long skuId) {
        return ApiResponse.success(priceOverrideService.listOverrides(skuId));
    }

    @PostMapping
    public ApiResponse<SkuPriceOverrideDto> createOverride(@Valid @RequestBody UpsertSkuPriceOverrideRequest request) {
        return ApiResponse.success(priceOverrideService.createOverride(
                request.skuId(), request.storeId(), request.priceContext(),
                request.priceContextRef(), request.overridePriceCents(), request.active()));
    }

    @DeleteMapping("/{overrideId}")
    public ApiResponse<Void> deleteOverride(@PathVariable Long overrideId) {
        priceOverrideService.deleteOverride(overrideId);
        return ApiResponse.success(null);
    }
}
