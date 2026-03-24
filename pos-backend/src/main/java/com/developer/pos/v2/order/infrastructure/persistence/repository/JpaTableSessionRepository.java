package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTableSessionRepository extends JpaRepository<TableSessionEntity, Long> {
    Optional<TableSessionEntity> findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(Long storeId, Long tableId, String sessionStatus);
}
