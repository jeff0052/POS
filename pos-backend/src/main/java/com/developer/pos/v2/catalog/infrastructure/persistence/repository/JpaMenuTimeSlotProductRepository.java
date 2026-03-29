package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface JpaMenuTimeSlotProductRepository extends JpaRepository<MenuTimeSlotProductEntity, Long> {
    List<MenuTimeSlotProductEntity> findByTimeSlotId(Long timeSlotId);

    List<MenuTimeSlotProductEntity> findByTimeSlotIdAndIsVisible(Long timeSlotId, boolean visible);

    Optional<MenuTimeSlotProductEntity> findByTimeSlotIdAndProductId(Long timeSlotId, Long productId);

    @Modifying
    void deleteByTimeSlotId(Long timeSlotId);
}
