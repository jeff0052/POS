package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaModifierGroupRepository extends JpaRepository<ModifierGroupEntity, Long> {
    List<ModifierGroupEntity> findByMerchantIdOrderBySortOrderAscGroupNameAsc(Long merchantId);

    List<ModifierGroupEntity> findByIdInOrderBySortOrderAsc(List<Long> ids);

    Optional<ModifierGroupEntity> findByMerchantIdAndGroupCode(Long merchantId, String groupCode);
}
