package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSettlementRecordRepository extends JpaRepository<SettlementRecordEntity, Long> {
}
