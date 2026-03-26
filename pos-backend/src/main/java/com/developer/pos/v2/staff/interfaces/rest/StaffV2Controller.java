package com.developer.pos.v2.staff.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.staff.application.command.CreateStaffCommand;
import com.developer.pos.v2.staff.application.command.UpdateStaffCommand;
import com.developer.pos.v2.staff.application.dto.StaffDto;
import com.developer.pos.v2.staff.application.dto.StaffPinVerificationResult;
import com.developer.pos.v2.staff.application.service.StaffApplicationService;
import com.developer.pos.v2.staff.interfaces.rest.request.CreateStaffRequest;
import com.developer.pos.v2.staff.interfaces.rest.request.UpdateStaffRequest;
import com.developer.pos.v2.staff.interfaces.rest.request.VerifyPinRequest;
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
@RequestMapping("/api/v2/staff")
public class StaffV2Controller implements V2Api {

    private final StaffApplicationService staffApplicationService;

    public StaffV2Controller(StaffApplicationService staffApplicationService) {
        this.staffApplicationService = staffApplicationService;
    }

    @PostMapping
    public ApiResponse<StaffDto> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        CreateStaffCommand command = new CreateStaffCommand(
                request.merchantId(),
                request.storeId(),
                request.staffName(),
                request.staffCode(),
                request.pin(),
                request.roleCode(),
                request.phone()
        );
        return ApiResponse.success(staffApplicationService.createStaff(command));
    }

    @GetMapping
    public ApiResponse<List<StaffDto>> listStaff(@RequestParam Long storeId) {
        return ApiResponse.success(staffApplicationService.listStaff(storeId));
    }

    @GetMapping("/{staffId}")
    public ApiResponse<StaffDto> getStaff(@PathVariable String staffId) {
        return ApiResponse.success(staffApplicationService.getStaff(staffId));
    }

    @PutMapping("/{staffId}")
    public ApiResponse<StaffDto> updateStaff(
            @PathVariable String staffId,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        UpdateStaffCommand command = new UpdateStaffCommand(
                staffId,
                request.staffName(),
                request.roleCode(),
                request.phone(),
                request.staffStatus()
        );
        return ApiResponse.success(staffApplicationService.updateStaff(command));
    }

    @DeleteMapping("/{staffId}")
    public ApiResponse<Void> deactivateStaff(@PathVariable String staffId) {
        staffApplicationService.deactivateStaff(staffId);
        return ApiResponse.success(null);
    }

    @PostMapping("/verify-pin")
    public ApiResponse<StaffPinVerificationResult> verifyPin(@Valid @RequestBody VerifyPinRequest request) {
        return ApiResponse.success(
                staffApplicationService.verifyPin(request.storeId(), request.staffCode(), request.pin())
        );
    }
}
