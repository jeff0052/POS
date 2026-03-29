package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStoreTableRepository extends JpaRepository<StoreTableEntity, Long> {
    Optional<StoreTableEntity> findByIdAndStoreId(Long id, Long storeId);

    default Optional<StoreTableEntity> findByStoreIdAndId(Long storeId, Long id) {
        return findByIdAndStoreId(id, storeId);
    }

    Optional<StoreTableEntity> findByStoreIdAndTableCode(Long storeId, String tableCode);

    List<StoreTableEntity> findAllByStoreIdOrderByIdAsc(Long storeId);
}
