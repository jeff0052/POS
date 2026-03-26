package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaSettlementRecordRepository extends JpaRepository<SettlementRecordEntity, Long> {
    boolean existsByActiveOrderId(String activeOrderId);
    Optional<SettlementRecordEntity> findByActiveOrderId(String activeOrderId);
}
