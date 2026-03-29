package com.developer.pos.v2.store.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.audit.application.annotation.Audited;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.application.dto.TableMergeResultDto;
import com.developer.pos.v2.store.application.dto.TableUnmergeResultDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.TableMergeRecordEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaTableMergeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class TableMergeApplicationService implements UseCase {

    private final JpaStoreTableRepository storeTableRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaTableMergeRecordRepository mergeRecordRepository;

    public TableMergeApplicationService(
            JpaStoreTableRepository storeTableRepository,
            JpaTableSessionRepository tableSessionRepository,
            JpaTableMergeRecordRepository mergeRecordRepository
    ) {
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.mergeRecordRepository = mergeRecordRepository;
    }

    @Transactional
    @Audited(action = "MERGE_TABLE", targetType = "STORE_TABLE", riskLevel = "MEDIUM")
    public TableMergeResultDto merge(Long storeId, Long masterTableId, Long mergedTableId) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasStoreAccess(storeId)) {
            throw new IllegalArgumentException("No access to store: " + storeId);
        }

        if (masterTableId.equals(mergedTableId)) {
            throw new IllegalArgumentException("Cannot merge a table with itself.");
        }

        StoreTableEntity masterTable = storeTableRepository.findByIdAndStoreId(masterTableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Master table not found."));
        StoreTableEntity mergedTable = storeTableRepository.findByIdAndStoreId(mergedTableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Merged table not found."));

        if (!"OCCUPIED".equals(masterTable.getTableStatus())) {
            throw new IllegalStateException("Master table must be OCCUPIED, current: " + masterTable.getTableStatus());
        }
        if (!"OCCUPIED".equals(mergedTable.getTableStatus())) {
            throw new IllegalStateException("Merged table must be OCCUPIED, current: " + mergedTable.getTableStatus());
        }

        TableSessionEntity masterSession = tableSessionRepository
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, masterTableId, "OPEN")
                .orElseThrow(() -> new IllegalStateException("No open session for master table."));
        TableSessionEntity mergedSession = tableSessionRepository
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, mergedTableId, "OPEN")
                .orElseThrow(() -> new IllegalStateException("No open session for merged table."));

        if (masterSession.getMergedIntoSessionId() != null) {
            throw new IllegalStateException("Master table is itself merged into another table. Unmerge it first.");
        }

        if (mergedSession.getMergedIntoSessionId() != null) {
            throw new IllegalStateException("Merged table is already merged into another table.");
        }

        // Set session pointer
        mergedSession.setMergedIntoSessionId(masterSession.getId());
        tableSessionRepository.saveAndFlush(mergedSession);

        // Create merge record for audit/history
        TableMergeRecordEntity record = new TableMergeRecordEntity(
                storeId, masterTableId, masterSession.getId(),
                mergedTableId, mergedSession.getId());
        mergeRecordRepository.saveAndFlush(record);

        mergedTable.setTableStatus("MERGED");
        storeTableRepository.saveAndFlush(mergedTable);

        return new TableMergeResultDto(record.getId(), masterSession.getId());
    }

    @Transactional
    @Audited(action = "UNMERGE_TABLE", targetType = "STORE_TABLE", riskLevel = "MEDIUM")
    public TableUnmergeResultDto unmerge(Long storeId, Long mergeRecordId) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasStoreAccess(storeId)) {
            throw new IllegalArgumentException("No access to store: " + storeId);
        }

        // Look up via table_merge_records, not table_sessions
        TableMergeRecordEntity record = mergeRecordRepository.findByIdAndStoreId(mergeRecordId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Merge record not found: " + mergeRecordId));

        if (!"ACTIVE".equals(record.getMergeStatus())) {
            throw new IllegalStateException("Merge record is not active, current: " + record.getMergeStatus());
        }

        TableSessionEntity mergedSession = tableSessionRepository.findById(record.getMergedSessionId())
                .orElseThrow(() -> new IllegalStateException("Merged session not found."));

        if (!"OPEN".equals(mergedSession.getSessionStatus())) {
            throw new IllegalStateException("Cannot unmerge a closed session.");
        }

        // Clear session pointer
        mergedSession.setMergedIntoSessionId(null);
        tableSessionRepository.saveAndFlush(mergedSession);

        // Update merge record
        record.setMergeStatus("UNMERGED");
        record.setUnmergedAt(OffsetDateTime.now());
        record.setUnmergedBy(actor.userId());
        mergeRecordRepository.saveAndFlush(record);

        StoreTableEntity mergedTable = storeTableRepository.findByIdAndStoreId(mergedSession.getTableId(), storeId)
                .orElseThrow(() -> new IllegalStateException("Merged table not found."));
        mergedTable.setTableStatus("OCCUPIED");
        storeTableRepository.saveAndFlush(mergedTable);

        return new TableUnmergeResultDto(true);
    }
}
