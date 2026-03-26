package com.developer.pos.v2.staff.infrastructure.persistence.repository;

import com.developer.pos.v2.staff.infrastructure.persistence.entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaStaffRepository extends JpaRepository<StaffEntity, Long> {
    Optional<StaffEntity> findByStaffId(String staffId);
    Optional<StaffEntity> findByStoreIdAndStaffCode(Long storeId, String staffCode);
    List<StaffEntity> findByStoreIdAndStaffStatusOrderByStaffCodeAsc(Long storeId, String staffStatus);
}
