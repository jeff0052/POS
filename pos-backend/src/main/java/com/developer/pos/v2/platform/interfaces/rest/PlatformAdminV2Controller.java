package com.developer.pos.v2.platform.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.platform.application.command.CreateStoreCommand;
import com.developer.pos.v2.platform.application.dto.PlatformDashboardDto;
import com.developer.pos.v2.platform.application.dto.StoreSummaryDto;
import com.developer.pos.v2.platform.application.service.PlatformAdminApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/platform")
public class PlatformAdminV2Controller implements V2Api {

    private final PlatformAdminApplicationService service;

    public PlatformAdminV2Controller(PlatformAdminApplicationService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ApiResponse<PlatformDashboardDto> dashboard() {
        return ApiResponse.success(service.getDashboard());
    }

    @GetMapping("/stores")
    public ApiResponse<List<StoreSummaryDto>> listStores() {
        return ApiResponse.success(service.listStores());
    }

    @GetMapping("/stores/{storeId}")
    public ApiResponse<StoreSummaryDto> getStore(@PathVariable Long storeId) {
        return ApiResponse.success(service.getStore(storeId));
    }

    @PostMapping("/stores")
    public ApiResponse<StoreSummaryDto> createStore(@RequestBody CreateStoreCommand command) {
        return ApiResponse.success(service.createStore(command));
    }

    @PutMapping("/stores/{storeId}/status")
    public ApiResponse<StoreSummaryDto> updateStoreStatus(@PathVariable Long storeId, @RequestBody StatusUpdateRequest request) {
        return ApiResponse.success(service.updateStoreStatus(storeId, request.status()));
    }

    public record StatusUpdateRequest(String status) {}
}
