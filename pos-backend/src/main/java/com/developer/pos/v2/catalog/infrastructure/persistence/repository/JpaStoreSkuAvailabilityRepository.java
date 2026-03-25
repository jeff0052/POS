package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStoreSkuAvailabilityRepository extends JpaRepository<StoreSkuAvailabilityEntity, Long> {
    List<StoreSkuAvailabilityEntity> findByStoreIdAndSkuIdIn(Long storeId, List<Long> skuIds);

    Optional<StoreSkuAvailabilityEntity> findByStoreIdAndSkuId(Long storeId, Long skuId);
}
