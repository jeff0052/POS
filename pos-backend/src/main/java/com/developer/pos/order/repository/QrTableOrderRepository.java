package com.developer.pos.order.repository;

import com.developer.pos.order.entity.QrTableOrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrTableOrderRepository extends JpaRepository<QrTableOrderEntity, Long> {
    Optional<QrTableOrderEntity> findTopByStoreCodeAndTableCodeOrderByCreatedAtDesc(String storeCode, String tableCode);
    Optional<QrTableOrderEntity> findTopByStoreCodeAndTableCodeAndSettlementStatusNotOrderByCreatedAtDesc(
        String storeCode,
        String tableCode,
        String settlementStatus
    );
    List<QrTableOrderEntity> findTop50ByOrderByCreatedAtDesc();
}
