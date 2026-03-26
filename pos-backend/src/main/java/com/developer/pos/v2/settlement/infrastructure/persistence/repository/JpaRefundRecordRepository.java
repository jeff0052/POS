package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRefundRecordRepository extends JpaRepository<RefundRecordEntity, Long> {

    Optional<RefundRecordEntity> findByRefundNo(String refundNo);

    List<RefundRecordEntity> findBySettlementId(Long settlementId);

    Page<RefundRecordEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Page<RefundRecordEntity> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);
}
