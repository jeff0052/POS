package com.developer.pos.store.repository;

import com.developer.pos.store.entity.StoreSettingsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettingsEntity, Long> {
    Optional<StoreSettingsEntity> findByStoreId(Long storeId);
}
