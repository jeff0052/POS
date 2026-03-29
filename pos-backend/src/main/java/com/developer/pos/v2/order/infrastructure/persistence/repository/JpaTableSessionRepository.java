package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaTableSessionRepository extends JpaRepository<TableSessionEntity, Long> {
    Optional<TableSessionEntity> findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(Long storeId, Long tableId, String sessionStatus);

    List<TableSessionEntity> findAllByStoreIdOrderByIdDesc(Long storeId);

    List<TableSessionEntity> findAllByMergedIntoSessionIdAndSessionStatus(Long mergedIntoSessionId, String sessionStatus);

    @Query("SELECT s FROM TableSessionEntity s WHERE s.tableId = :tableId AND s.sessionStatus = 'ACTIVE' ORDER BY s.id DESC")
    Optional<TableSessionEntity> findActiveByTableId(@Param("tableId") Long tableId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TableSessionEntity s WHERE s.tableId = :tableId AND s.sessionStatus = 'ACTIVE'")
    Optional<TableSessionEntity> findActiveByTableIdForUpdate(@Param("tableId") Long tableId);

    List<TableSessionEntity> findAllByMergedIntoSessionId(Long masterSessionId);
}
