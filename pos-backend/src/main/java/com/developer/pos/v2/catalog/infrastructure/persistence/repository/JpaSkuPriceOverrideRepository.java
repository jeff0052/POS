package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuPriceOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSkuPriceOverrideRepository extends JpaRepository<SkuPriceOverrideEntity, Long> {
    List<SkuPriceOverrideEntity> findBySkuIdAndIsActive(Long skuId, boolean active);

    List<SkuPriceOverrideEntity> findBySkuIdInAndIsActive(List<Long> skuIds, boolean active);

    List<SkuPriceOverrideEntity> findBySkuIdAndStoreIdAndIsActive(Long skuId, Long storeId, boolean active);
}
