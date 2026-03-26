package com.developer.pos.v2.staff.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.staff.application.command.CreateStaffCommand;
import com.developer.pos.v2.staff.application.command.UpdateStaffCommand;
import com.developer.pos.v2.staff.application.dto.StaffDto;
import com.developer.pos.v2.staff.application.dto.StaffPinVerificationResult;
import com.developer.pos.v2.staff.application.service.StaffApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/staff")
public class StaffV2Controller implements V2Api {

    private final StaffApplicationService service;

    public StaffV2Controller(StaffApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<StaffDto> createStaff(@RequestBody CreateStaffCommand command) {
        return ApiResponse.success(service.createStaff(command));
    }

    @GetMapping
    public ApiResponse<List<StaffDto>> listStaff(@RequestParam Long storeId) {
        return ApiResponse.success(service.listStaff(storeId));
    }

    @GetMapping("/{staffId}")
    public ApiResponse<StaffDto> getStaff(@PathVariable String staffId) {
        return ApiResponse.success(service.getStaff(staffId));
    }

    @PutMapping("/{staffId}")
    public ApiResponse<StaffDto> updateStaff(@PathVariable String staffId, @RequestBody UpdateStaffCommand command) {
        return ApiResponse.success(service.updateStaff(new UpdateStaffCommand(
                staffId, command.staffName(), command.roleCode(), command.phone(), command.staffStatus()
        )));
    }

    @DeleteMapping("/{staffId}")
    public ApiResponse<Void> deactivateStaff(@PathVariable String staffId) {
        service.deactivateStaff(staffId);
        return ApiResponse.success(null);
    }

    @PostMapping("/verify-pin")
    public ApiResponse<StaffPinVerificationResult> verifyPin(@RequestBody VerifyPinRequest request) {
        return ApiResponse.success(service.verifyPin(request.storeId(), request.staffCode(), request.pin()));
    }

    public record VerifyPinRequest(Long storeId, String staffCode, String pin) {}
}
