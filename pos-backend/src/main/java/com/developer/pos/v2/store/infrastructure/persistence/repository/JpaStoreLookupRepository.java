package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaStoreLookupRepository extends JpaRepository<StoreEntity, Long> {
    Optional<StoreEntity> findByStoreCode(String storeCode);
}
