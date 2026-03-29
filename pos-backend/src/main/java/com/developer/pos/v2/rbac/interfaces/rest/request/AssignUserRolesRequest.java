package com.developer.pos.v2.rbac.interfaces.rest.request;

import java.util.List;

public class AssignUserRolesRequest {

    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
