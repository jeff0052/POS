package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaBuffetPackageRepository extends JpaRepository<BuffetPackageEntity, Long> {
    List<BuffetPackageEntity> findByStoreIdOrderBySortOrderAsc(Long storeId);
    List<BuffetPackageEntity> findByStoreIdAndPackageStatusOrderBySortOrderAsc(Long storeId, String status);
    Optional<BuffetPackageEntity> findByStoreIdAndPackageCode(Long storeId, String packageCode);
}
