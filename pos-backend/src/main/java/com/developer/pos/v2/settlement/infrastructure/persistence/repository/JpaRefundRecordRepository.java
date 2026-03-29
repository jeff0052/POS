package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaRefundRecordRepository extends JpaRepository<RefundRecordEntity, Long> {

    Optional<RefundRecordEntity> findByRefundNo(String refundNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundRecordEntity r WHERE r.refundNo = :refundNo")
    Optional<RefundRecordEntity> findByRefundNoForUpdate(@Param("refundNo") String refundNo);

    List<RefundRecordEntity> findBySettlementId(Long settlementId);

    Page<RefundRecordEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Page<RefundRecordEntity> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);
}
