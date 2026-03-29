package com.developer.pos.v2.rbac.interfaces.rest.request;

import java.util.List;

public class UpdateCustomRoleRequest {

    private String roleName;
    private String roleDescription;
    private Long maxRefundCents;
    private List<String> permissionCodes;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public Long getMaxRefundCents() {
        return maxRefundCents;
    }

    public void setMaxRefundCents(Long maxRefundCents) {
        this.maxRefundCents = maxRefundCents;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
