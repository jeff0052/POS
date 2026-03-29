package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaSupplierRepository extends JpaRepository<SupplierEntity, Long> {
    List<SupplierEntity> findByMerchantIdAndSupplierStatus(Long merchantId, String supplierStatus);
}
