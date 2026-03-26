package com.developer.pos.v2.staff.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleCode;
    private String permissionCode;

    public Long getId() { return id; }
    public String getRoleCode() { return roleCode; }
    public String getPermissionCode() { return permissionCode; }
}
