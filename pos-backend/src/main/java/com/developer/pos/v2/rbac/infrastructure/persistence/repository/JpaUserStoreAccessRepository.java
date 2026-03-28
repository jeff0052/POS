package com.developer.pos.v2.rbac.infrastructure.persistence.repository;

import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserStoreAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserStoreAccessRepository extends JpaRepository<UserStoreAccessEntity, Long> {

    List<UserStoreAccessEntity> findByUserId(Long userId);

    Optional<UserStoreAccessEntity> findByUserIdAndStoreId(Long userId, Long storeId);

    void deleteByUserIdAndStoreId(Long userId, Long storeId);
}
