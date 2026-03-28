package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaUserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
