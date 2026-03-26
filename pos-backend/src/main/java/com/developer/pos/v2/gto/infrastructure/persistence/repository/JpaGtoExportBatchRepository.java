package com.developer.pos.v2.gto.infrastructure.persistence.repository;

import com.developer.pos.v2.gto.infrastructure.persistence.entity.GtoExportBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaGtoExportBatchRepository extends JpaRepository<GtoExportBatchEntity, Long> {

    Optional<GtoExportBatchEntity> findByBatchId(String batchId);

    Optional<GtoExportBatchEntity> findByStoreIdAndExportDate(Long storeId, LocalDate exportDate);

    Page<GtoExportBatchEntity> findByMerchantIdOrderByExportDateDesc(Long merchantId, Pageable pageable);
}
