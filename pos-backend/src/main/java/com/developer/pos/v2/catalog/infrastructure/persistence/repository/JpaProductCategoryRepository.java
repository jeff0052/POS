package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Long> {
    List<ProductCategoryEntity> findByStoreIdOrderBySortOrderAscCategoryNameAsc(Long storeId);
}
