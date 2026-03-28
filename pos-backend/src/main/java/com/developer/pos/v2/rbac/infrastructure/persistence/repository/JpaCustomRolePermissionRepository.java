package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCustomRolePermissionRepository extends JpaRepository<CustomRolePermissionEntity, Long> {

    List<CustomRolePermissionEntity> findByRoleId(Long roleId);

    List<CustomRolePermissionEntity> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
