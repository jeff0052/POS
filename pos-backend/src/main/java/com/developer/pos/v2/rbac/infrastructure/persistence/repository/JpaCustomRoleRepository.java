package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaCustomRoleRepository extends JpaRepository<CustomRoleEntity, Long> {

    Optional<CustomRoleEntity> findByRoleCode(String roleCode);

    List<CustomRoleEntity> findByMerchantIdOrMerchantIdIsNull(Long merchantId);

    List<CustomRoleEntity> findByMerchantIdIsNull();
}
