package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuModifierGroupBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface JpaSkuModifierGroupBindingRepository extends JpaRepository<SkuModifierGroupBindingEntity, Long> {
    List<SkuModifierGroupBindingEntity> findBySkuIdOrderBySortOrderAsc(Long skuId);

    List<SkuModifierGroupBindingEntity> findBySkuIdInOrderBySkuIdAscSortOrderAsc(List<Long> skuIds);

    Optional<SkuModifierGroupBindingEntity> findBySkuIdAndModifierGroupId(Long skuId, Long modifierGroupId);

    @Modifying
    void deleteBySkuIdAndModifierGroupId(Long skuId, Long modifierGroupId);

    @Modifying
    void deleteBySkuId(Long skuId);
}
