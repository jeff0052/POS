package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSkuRepository extends JpaRepository<SkuEntity, Long> {
    List<SkuEntity> findByProductIdOrderByIdAsc(Long productId);

    List<SkuEntity> findByProductIdInOrderByProductIdAscIdAsc(List<Long> productIds);

    long countByImageId(String imageId);
}
