package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaModifierOptionRepository extends JpaRepository<ModifierOptionEntity, Long> {
    List<ModifierOptionEntity> findByGroupIdOrderBySortOrderAsc(Long groupId);

    List<ModifierOptionEntity> findByGroupIdInOrderByGroupIdAscSortOrderAsc(List<Long> groupIds);

    void deleteByGroupId(Long groupId);
}
