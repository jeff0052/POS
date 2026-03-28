package com.developer.pos.v2.rbac.infrastructure.persistence.entity;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.ActionContextAuditListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "RbacPermissionEntity")
@Table(name = "permissions")
@EntityListeners(ActionContextAuditListener.class)
public class PermissionEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_code", nullable = false, unique = true)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false)
    private String permissionName;

    @Column(name = "permission_group", nullable = false)
    private String permissionGroup;

    @Column(name = "description")
    private String description;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel = "LOW";

    public PermissionEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionGroup() {
        return permissionGroup;
    }

    public void setPermissionGroup(String permissionGroup) {
        this.permissionGroup = permissionGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
