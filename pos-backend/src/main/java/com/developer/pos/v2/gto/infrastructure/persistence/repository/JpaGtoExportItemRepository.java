package com.developer.pos.v2.gto.infrastructure.persistence.repository;

import com.developer.pos.v2.gto.infrastructure.persistence.entity.GtoExportItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaGtoExportItemRepository extends JpaRepository<GtoExportItemEntity, Long> {

    List<GtoExportItemEntity> findByBatchId(Long batchId);
}
