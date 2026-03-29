package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, Long> {

    List<PermissionEntity> findByPermissionGroup(String permissionGroup);
}
