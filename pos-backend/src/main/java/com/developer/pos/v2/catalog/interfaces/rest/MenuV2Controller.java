package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto;
import com.developer.pos.v2.catalog.application.dto.MenuTimeSlotDto;
import com.developer.pos.v2.catalog.application.service.MenuQueryService;
import com.developer.pos.v2.catalog.application.service.MenuTimeSlotManagementService;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpsertMenuTimeSlotRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/stores/{storeId}")
public class MenuV2Controller implements V2Api {

    private final MenuQueryService menuQueryService;
    private final MenuTimeSlotManagementService timeSlotManagementService;

    public MenuV2Controller(MenuQueryService menuQueryService,
                            MenuTimeSlotManagementService timeSlotManagementService) {
        this.menuQueryService = menuQueryService;
        this.timeSlotManagementService = timeSlotManagementService;
    }

    // ─── Menu Query ─────────────────────────────────────────────────────

    @GetMapping("/menu")
    public ApiResponse<MenuQueryResultDto> queryMenu(
            @PathVariable Long storeId,
            @RequestParam(required = false) String diningMode,
            @RequestParam(required = false) Long timeSlotId
    ) {
        return ApiResponse.success(menuQueryService.queryMenu(storeId, diningMode, timeSlotId));
    }

    // ─── Time Slot Management ───────────────────────────────────────────

    @GetMapping("/menu-time-slots")
    public ApiResponse<List<MenuTimeSlotDto>> listTimeSlots(@PathVariable Long storeId) {
        return ApiResponse.success(timeSlotManagementService.listSlots(storeId));
    }

    @GetMapping("/menu-time-slots/{slotId}")
    public ApiResponse<MenuTimeSlotDto> getTimeSlot(@PathVariable Long storeId, @PathVariable Long slotId) {
        return ApiResponse.success(timeSlotManagementService.getSlot(slotId, storeId));
    }

    @PostMapping("/menu-time-slots")
    public ApiResponse<MenuTimeSlotDto> createTimeSlot(
            @PathVariable Long storeId,
            @Valid @RequestBody UpsertMenuTimeSlotRequest request
    ) {
        return ApiResponse.success(timeSlotManagementService.createSlot(
                storeId, request.slotCode(), request.slotName(),
                request.startTime(), request.endTime(),
                request.applicableDays(), request.diningModes(),
                request.active(), request.priority(), request.productIds()));
    }

    @PutMapping("/menu-time-slots/{slotId}")
    public ApiResponse<MenuTimeSlotDto> updateTimeSlot(
            @PathVariable Long storeId,
            @PathVariable Long slotId,
            @Valid @RequestBody UpsertMenuTimeSlotRequest request
    ) {
        return ApiResponse.success(timeSlotManagementService.updateSlot(
                slotId, request.slotCode(), request.slotName(),
                request.startTime(), request.endTime(),
                request.applicableDays(), request.diningModes(),
                request.active(), request.priority(), request.productIds()));
    }

    @DeleteMapping("/menu-time-slots/{slotId}")
    public ApiResponse<Void> deleteTimeSlot(@PathVariable Long storeId, @PathVariable Long slotId) {
        timeSlotManagementService.deleteSlot(slotId, storeId);
        return ApiResponse.success(null);
    }
}
