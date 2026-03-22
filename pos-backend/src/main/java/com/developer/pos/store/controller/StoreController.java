package com.developer.pos.store.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.store.dto.StoreDto;
import com.developer.pos.store.dto.StoreSettingsDto;
import com.developer.pos.store.service.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores/current")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ApiResponse<StoreDto> currentStore() {
        return ApiResponse.success(storeService.getCurrentStore());
    }

    @GetMapping("/settings")
    public ApiResponse<StoreSettingsDto> settings() {
        return ApiResponse.success(storeService.getCurrentSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<StoreSettingsDto> updateSettings(@RequestBody StoreSettingsDto request) {
        return ApiResponse.success(storeService.updateSettings(request));
    }
}
