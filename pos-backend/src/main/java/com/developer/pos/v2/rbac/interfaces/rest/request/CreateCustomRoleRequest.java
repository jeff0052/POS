package com.developer.pos.v2.rbac.interfaces.rest.request;

import java.util.List;

public class CreateCustomRoleRequest {

    private Long merchantId;
    private String roleCode;
    private String roleName;
    private String roleDescription;
    private String roleLevel;
    private Long maxRefundCents;
    private List<String> permissionCodes;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

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

    public String getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(String roleLevel) {
        this.roleLevel = roleLevel;
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
