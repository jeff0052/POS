package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStoreRepository extends JpaRepository<StoreEntity, Long> {
}
