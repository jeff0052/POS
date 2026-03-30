package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.SopImportBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaSopImportBatchRepository extends JpaRepository<SopImportBatchEntity, Long> {
    List<SopImportBatchEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
