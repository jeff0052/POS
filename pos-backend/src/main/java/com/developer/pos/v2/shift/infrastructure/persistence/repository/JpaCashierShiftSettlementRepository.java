package com.developer.pos.v2.shift.infrastructure.persistence.repository;

import com.developer.pos.v2.shift.infrastructure.persistence.entity.CashierShiftSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaCashierShiftSettlementRepository extends JpaRepository<CashierShiftSettlementEntity, Long> {
    List<CashierShiftSettlementEntity> findByShiftIdOrderBySettledAtAsc(Long shiftId);
}
