package com.developer.pos.v2.platform.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.platform.application.dto.PlatformStoreOverviewDto;
import com.developer.pos.v2.platform.application.service.PlatformStoreReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/platform/stores")
public class PlatformStoreV2Controller implements V2Api {

    private final PlatformStoreReadService platformStoreReadService;

    public PlatformStoreV2Controller(PlatformStoreReadService platformStoreReadService) {
        this.platformStoreReadService = platformStoreReadService;
    }

    @GetMapping("/overview")
    public ApiResponse<List<PlatformStoreOverviewDto>> listStoreOverview() {
        return ApiResponse.success(platformStoreReadService.listStoreOverview());
    }
}
