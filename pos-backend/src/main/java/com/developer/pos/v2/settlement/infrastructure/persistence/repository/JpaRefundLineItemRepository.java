package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundLineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRefundLineItemRepository extends JpaRepository<RefundLineItemEntity, Long> {
    List<RefundLineItemEntity> findByRefundId(Long refundId);
}
