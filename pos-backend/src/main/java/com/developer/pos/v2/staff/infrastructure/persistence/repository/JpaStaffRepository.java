package com.developer.pos.v2.staff.infrastructure.persistence.repository;

import com.developer.pos.v2.staff.infrastructure.persistence.entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStaffRepository extends JpaRepository<StaffEntity, Long> {

    List<StaffEntity> findByStoreIdAndStaffStatus(Long storeId, String staffStatus);

    Optional<StaffEntity> findByStaffId(String staffId);

    Optional<StaffEntity> findByStoreIdAndStaffCode(Long storeId, String staffCode);
}
