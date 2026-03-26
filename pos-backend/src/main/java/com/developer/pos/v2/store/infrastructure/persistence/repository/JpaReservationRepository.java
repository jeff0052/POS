package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaReservationRepository extends JpaRepository<ReservationEntity, Long> {
    List<ReservationEntity> findAllByStoreIdOrderByIdDesc(Long storeId);

    Optional<ReservationEntity> findByIdAndStoreId(Long id, Long storeId);
}
