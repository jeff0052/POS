package com.developer.pos.v2.shift.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.shift.application.command.CloseShiftCommand;
import com.developer.pos.v2.shift.application.command.OpenShiftCommand;
import com.developer.pos.v2.shift.application.dto.CashierShiftDto;
import com.developer.pos.v2.shift.application.service.CashierShiftApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/shifts")
public class CashierShiftV2Controller implements V2Api {

    private final CashierShiftApplicationService service;

    public CashierShiftV2Controller(CashierShiftApplicationService service) {
        this.service = service;
    }

    @PostMapping("/open")
    public ApiResponse<CashierShiftDto> openShift(@RequestBody OpenShiftCommand command) {
        return ApiResponse.success(service.openShift(command));
    }

    @PostMapping("/{shiftId}/close")
    public ApiResponse<CashierShiftDto> closeShift(@PathVariable String shiftId, @RequestBody CloseShiftCommand command) {
        return ApiResponse.success(service.closeShift(new CloseShiftCommand(shiftId, command.closingCashCents(), command.notes())));
    }

    @GetMapping("/current")
    public ApiResponse<CashierShiftDto> getCurrentShift(@RequestParam Long storeId, @RequestParam String cashierStaffId) {
        return ApiResponse.success(service.getCurrentShift(storeId, cashierStaffId));
    }

    @GetMapping("/{shiftId}")
    public ApiResponse<CashierShiftDto> getShiftDetail(@PathVariable String shiftId) {
        return ApiResponse.success(service.getShiftDetail(shiftId));
    }

    @GetMapping
    public ApiResponse<Page<CashierShiftDto>> listShifts(@RequestParam Long storeId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listShifts(storeId, page, size));
    }
}
