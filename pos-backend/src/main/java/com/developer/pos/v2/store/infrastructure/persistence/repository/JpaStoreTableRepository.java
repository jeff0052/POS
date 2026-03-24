package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaStoreTableRepository extends JpaRepository<StoreTableEntity, Long> {
    Optional<StoreTableEntity> findByIdAndStoreId(Long id, Long storeId);

    Optional<StoreTableEntity> findByStoreIdAndTableCode(Long storeId, String tableCode);
}
