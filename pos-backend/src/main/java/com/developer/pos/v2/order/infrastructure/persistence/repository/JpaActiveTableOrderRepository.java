package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaActiveTableOrderRepository extends JpaRepository<ActiveTableOrderEntity, Long> {
    Optional<ActiveTableOrderEntity> findByStoreIdAndTableId(Long storeId, Long tableId);

    Optional<ActiveTableOrderEntity> findByActiveOrderId(String activeOrderId);

    List<ActiveTableOrderEntity> findAllByStoreIdOrderByIdDesc(Long storeId);
}
