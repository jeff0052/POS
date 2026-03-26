package com.developer.pos.v2.staff.infrastructure.persistence.repository;

import com.developer.pos.v2.staff.infrastructure.persistence.entity.CashierShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCashierShiftRepository extends JpaRepository<CashierShiftEntity, Long> {
    Optional<CashierShiftEntity> findByShiftIdAndStoreId(String shiftId, Long storeId);

    Optional<CashierShiftEntity> findFirstByStoreIdAndCashierIdAndShiftStatusOrderByIdDesc(Long storeId, Long cashierId, String shiftStatus);
}
