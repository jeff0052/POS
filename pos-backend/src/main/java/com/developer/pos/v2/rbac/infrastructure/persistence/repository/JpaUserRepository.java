package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUserCodeAndMerchantId(String userCode, Long merchantId);

    List<UserEntity> findByMerchantId(Long merchantId);

    Optional<UserEntity> findByPhoneAndMerchantId(String phone, Long merchantId);
}
