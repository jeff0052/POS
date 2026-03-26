package com.developer.pos.v2.staff.infrastructure.persistence.repository;

import com.developer.pos.v2.staff.infrastructure.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {

    List<RolePermissionEntity> findByRoleCode(String roleCode);
}
