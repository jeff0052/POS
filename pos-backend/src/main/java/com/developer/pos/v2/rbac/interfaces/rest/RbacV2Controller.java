package com.developer.pos.v2.rbac.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.rbac.application.dto.PermissionGroupDto;
import com.developer.pos.v2.rbac.application.dto.RbacUserDetailDto;
import com.developer.pos.v2.rbac.application.dto.RbacUserDto;
import com.developer.pos.v2.rbac.application.service.RbacManagementService;
import com.developer.pos.v2.rbac.application.dto.CustomRoleDto;
import com.developer.pos.v2.rbac.interfaces.rest.request.AssignUserRolesRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.CreateCustomRoleRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.CreateUserRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.SetStoreAccessRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.UpdateCustomRoleRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.UpdateUserRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v2/rbac")
public class RbacV2Controller implements V2Api {

    private final RbacManagementService rbacManagementService;

    public RbacV2Controller(RbacManagementService rbacManagementService) {
        this.rbacManagementService = rbacManagementService;
    }

    // ==================== Permissions ====================

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionGroupDto>> listPermissions() {
        return ApiResponse.success(rbacManagementService.listPermissions());
    }

    // ==================== Roles ====================

    @GetMapping("/roles")
    public ApiResponse<List<CustomRoleDto>> listRoles(@RequestParam Long merchantId) {
        return ApiResponse.success(rbacManagementService.listRoles(merchantId));
    }

    @PostMapping("/roles")
    public ApiResponse<CustomRoleDto> createCustomRole(@RequestBody CreateCustomRoleRequest request) {
        return ApiResponse.success(rbacManagementService.createCustomRole(request));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<CustomRoleDto> updateCustomRole(
            @PathVariable Long roleId,
            @RequestBody UpdateCustomRoleRequest request
    ) {
        return ApiResponse.success(rbacManagementService.updateCustomRole(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Void> deleteCustomRole(@PathVariable Long roleId) {
        rbacManagementService.deleteCustomRole(roleId);
        return ApiResponse.success(null);
    }

    // ==================== Users ====================

    @GetMapping("/users")
    public ApiResponse<List<RbacUserDetailDto>> listUsers(@RequestParam Long merchantId) {
        return ApiResponse.success(rbacManagementService.listUsers(merchantId));
    }

    @PostMapping("/users")
    public ApiResponse<RbacUserDto> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.success(rbacManagementService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<RbacUserDto> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(rbacManagementService.updateUser(userId, request));
    }

    @PutMapping("/users/{userId}/deactivate")
    public ApiResponse<Void> deactivateUser(@PathVariable Long userId) {
        rbacManagementService.deactivateUser(userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body
    ) {
        rbacManagementService.resetPassword(userId, body.get("newPassword"));
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/pin")
    public ApiResponse<Void> setPin(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body
    ) {
        rbacManagementService.setPin(userId, body.get("newPin"));
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/roles")
    public ApiResponse<Void> assignRoles(
            @PathVariable Long userId,
            @RequestBody AssignUserRolesRequest request
    ) {
        rbacManagementService.assignRoles(userId, request.getRoleIds());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/store-access")
    public ApiResponse<Void> setStoreAccess(
            @PathVariable Long userId,
            @RequestBody SetStoreAccessRequest request
    ) {
        rbacManagementService.setStoreAccess(userId, request.getEntries());
        return ApiResponse.success(null);
    }
}
