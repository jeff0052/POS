package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSkuRepository extends JpaRepository<SkuEntity, Long> {
}
