package com.developer.pos.v2.kitchen.infrastructure.persistence.repository;

import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaKitchenStationRepository extends JpaRepository<KitchenStationEntity, Long> {

    List<KitchenStationEntity> findByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(
            Long storeId, String stationStatus);

    /** Returns the default station: lowest sort_order, then lowest id — fully deterministic */
    Optional<KitchenStationEntity> findFirstByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(
            Long storeId, String stationStatus);

    boolean existsByStoreIdAndStationCode(Long storeId, String stationCode);
}
