package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.TableMergeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTableMergeRecordRepository extends JpaRepository<TableMergeRecordEntity, Long> {

    Optional<TableMergeRecordEntity> findByIdAndStoreId(Long id, Long storeId);

    List<TableMergeRecordEntity> findAllByMasterSessionIdAndMergeStatus(Long masterSessionId, String mergeStatus);
}
