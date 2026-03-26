package com.developer.pos.v2.gto.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-only query repository for accessing settlement records needed by GTO export.
 * Kept separate to avoid modifying existing settlement repository.
 */
public interface GtoSettlementQueryRepository extends JpaRepository<SettlementRecordEntity, Long> {

    @Query(value = "SELECT s FROM SettlementRecordEntity s " +
            "WHERE s.storeId = :storeId " +
            "AND s.finalStatus = 'SETTLED' " +
            "AND FUNCTION('DATE', s.id) IS NOT NULL " +
            "ORDER BY s.paymentMethod")
    List<SettlementRecordEntity> findSettledByStoreId(@Param("storeId") Long storeId);

    @Query(value = "SELECT * FROM settlement_records " +
            "WHERE store_id = :storeId " +
            "AND final_status = 'SETTLED' " +
            "AND DATE(created_at) = :exportDate",
            nativeQuery = true)
    List<SettlementRecordEntity> findSettledByStoreIdAndDate(
            @Param("storeId") Long storeId,
            @Param("exportDate") String exportDate);
}
