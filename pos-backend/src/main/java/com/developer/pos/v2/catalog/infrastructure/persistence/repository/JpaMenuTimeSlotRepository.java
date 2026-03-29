package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaMenuTimeSlotRepository extends JpaRepository<MenuTimeSlotEntity, Long> {
    List<MenuTimeSlotEntity> findByStoreIdOrderByPriorityDescStartTimeAsc(Long storeId);

    List<MenuTimeSlotEntity> findByStoreIdAndIsActiveOrderByPriorityDescStartTimeAsc(Long storeId, boolean active);

    Optional<MenuTimeSlotEntity> findByStoreIdAndSlotCode(Long storeId, String slotCode);
}
