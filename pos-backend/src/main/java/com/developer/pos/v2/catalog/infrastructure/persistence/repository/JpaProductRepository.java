package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findByStoreIdOrderByProductNameAsc(Long storeId);

    List<ProductEntity> findByIdIn(List<Long> ids);
}
