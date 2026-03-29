package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.BuffetStatusDto;
import com.developer.pos.v2.catalog.application.service.BuffetSessionService;
import com.developer.pos.v2.catalog.interfaces.rest.request.StartBuffetRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/tables/{tableId}/buffet")
public class BuffetSessionV2Controller implements V2Api {

    private final BuffetSessionService sessionService;

    public BuffetSessionV2Controller(BuffetSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ApiResponse<BuffetStatusDto> startBuffet(
            @PathVariable Long storeId, @PathVariable Long tableId,
            @Valid @RequestBody StartBuffetRequest request) {
        return ApiResponse.success(sessionService.startBuffet(storeId, tableId, request));
    }

    @GetMapping("/status")
    public ApiResponse<BuffetStatusDto> getBuffetStatus(
            @PathVariable Long storeId, @PathVariable Long tableId) {
        return ApiResponse.success(sessionService.getBuffetStatus(storeId, tableId));
    }
}
