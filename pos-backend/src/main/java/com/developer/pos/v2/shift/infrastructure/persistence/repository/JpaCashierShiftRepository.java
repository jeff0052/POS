package com.developer.pos.v2.shift.infrastructure.persistence.repository;

import com.developer.pos.v2.shift.infrastructure.persistence.entity.CashierShiftEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaCashierShiftRepository extends JpaRepository<CashierShiftEntity, Long> {
    Optional<CashierShiftEntity> findByShiftId(String shiftId);
    Optional<CashierShiftEntity> findByStoreIdAndCashierStaffIdAndShiftStatus(Long storeId, String cashierStaffId, String shiftStatus);
    Page<CashierShiftEntity> findByStoreIdOrderByOpenedAtDesc(Long storeId, Pageable pageable);
}
