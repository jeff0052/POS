package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaActiveTableOrderRepository extends JpaRepository<ActiveTableOrderEntity, Long> {
    Optional<ActiveTableOrderEntity> findByStoreIdAndTableId(Long storeId, Long tableId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select activeOrder from ActiveTableOrderEntity activeOrder where activeOrder.storeId = :storeId and activeOrder.tableId = :tableId")
    Optional<ActiveTableOrderEntity> findByStoreIdAndTableIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("tableId") Long tableId
    );

    Optional<ActiveTableOrderEntity> findByActiveOrderId(String activeOrderId);

    List<ActiveTableOrderEntity> findAllByStoreIdOrderByIdDesc(Long storeId);
}
