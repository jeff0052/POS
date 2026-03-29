package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaBuffetPackageItemRepository extends JpaRepository<BuffetPackageItemEntity, Long> {
    List<BuffetPackageItemEntity> findByPackageIdOrderBySortOrderAsc(Long packageId);
    List<BuffetPackageItemEntity> findByPackageIdAndInclusionTypeNotOrderBySortOrderAsc(Long packageId, String excludedType);
    Optional<BuffetPackageItemEntity> findByPackageIdAndSkuId(Long packageId, Long skuId);
}
