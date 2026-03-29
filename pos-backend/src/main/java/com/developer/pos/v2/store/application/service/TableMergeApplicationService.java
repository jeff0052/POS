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
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableMergeApplicationService implements UseCase {

    private final JpaStoreTableRepository storeTableRepository;
    private final JpaTableSessionRepository tableSessionRepository;

    public TableMergeApplicationService(
            JpaStoreTableRepository storeTableRepository,
            JpaTableSessionRepository tableSessionRepository
    ) {
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
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

        mergedSession.setMergedIntoSessionId(masterSession.getId());
        tableSessionRepository.saveAndFlush(mergedSession);

        mergedTable.setTableStatus("MERGED");
        storeTableRepository.saveAndFlush(mergedTable);

        return new TableMergeResultDto(mergedSession.getId(), masterSession.getId());
    }

    @Transactional
    @Audited(action = "UNMERGE_TABLE", targetType = "STORE_TABLE", riskLevel = "MEDIUM")
    public TableUnmergeResultDto unmerge(Long storeId, Long mergeRecordId) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasStoreAccess(storeId)) {
            throw new IllegalArgumentException("No access to store: " + storeId);
        }

        TableSessionEntity mergedSession = tableSessionRepository.findById(mergeRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Merge record not found: " + mergeRecordId));

        if (!storeId.equals(mergedSession.getStoreId())) {
            throw new IllegalArgumentException("Merge record does not belong to store: " + storeId);
        }

        if (!"OPEN".equals(mergedSession.getSessionStatus())) {
            throw new IllegalStateException("Cannot unmerge a closed session.");
        }

        if (mergedSession.getMergedIntoSessionId() == null) {
            throw new IllegalStateException("Session is not merged.");
        }

        mergedSession.setMergedIntoSessionId(null);
        tableSessionRepository.saveAndFlush(mergedSession);

        StoreTableEntity mergedTable = storeTableRepository.findByIdAndStoreId(mergedSession.getTableId(), storeId)
                .orElseThrow(() -> new IllegalStateException("Merged table not found."));
        mergedTable.setTableStatus("OCCUPIED");
        storeTableRepository.saveAndFlush(mergedTable);

        return new TableUnmergeResultDto(true);
    }
}
