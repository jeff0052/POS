package com.developer.pos.v2.staff.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.staff.application.dto.ShiftClosedDto;
import com.developer.pos.v2.staff.application.dto.ShiftOpenedDto;
import com.developer.pos.v2.staff.application.dto.ShiftSummaryDto;
import com.developer.pos.v2.staff.application.service.ShiftApplicationService;
import com.developer.pos.v2.staff.interfaces.rest.request.CloseShiftRequest;
import com.developer.pos.v2.staff.interfaces.rest.request.OpenShiftRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/stores/{storeId}/shifts")
public class ShiftV2Controller implements V2Api {

    private final ShiftApplicationService shiftApplicationService;

    public ShiftV2Controller(ShiftApplicationService shiftApplicationService) {
        this.shiftApplicationService = shiftApplicationService;
    }

    @PostMapping("/open")
    public ApiResponse<ShiftOpenedDto> openShift(
            @PathVariable Long storeId,
            @Valid @RequestBody OpenShiftRequest request
    ) {
        return ApiResponse.success(
                shiftApplicationService.openShift(
                        storeId,
                        request.cashierId(),
                        request.cashierName(),
                        request.openingFloatCents()
                )
        );
    }

    @PostMapping("/{shiftId}/close")
    public ApiResponse<ShiftClosedDto> closeShift(
            @PathVariable Long storeId,
            @PathVariable String shiftId,
            @Valid @RequestBody CloseShiftRequest request
    ) {
        return ApiResponse.success(
                shiftApplicationService.closeShift(
                        storeId,
                        shiftId,
                        request.closingCashCents(),
                        request.closingNote()
                )
        );
    }

    @GetMapping("/{shiftId}/summary")
    public ApiResponse<ShiftSummaryDto> getShiftSummary(
            @PathVariable Long storeId,
            @PathVariable String shiftId
    ) {
        return ApiResponse.success(shiftApplicationService.getShiftSummary(storeId, shiftId));
    }
}
