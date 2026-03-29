package com.developer.pos.v2.store.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.audit.application.annotation.Audited;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.application.dto.QrTokenResultDto;
import com.developer.pos.v2.store.application.dto.TableCleanResultDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableCleanService implements UseCase {

    private final JpaStoreTableRepository storeTableRepository;
    private final QrTokenService qrTokenService;

    public TableCleanService(
            JpaStoreTableRepository storeTableRepository,
            QrTokenService qrTokenService
    ) {
        this.storeTableRepository = storeTableRepository;
        this.qrTokenService = qrTokenService;
    }

    @Transactional
    @Audited(action = "MARK_TABLE_CLEAN", targetType = "STORE_TABLE",
            targetIdExpression = "#tableId", riskLevel = "LOW")
    public TableCleanResultDto markClean(Long storeId, Long tableId) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasStoreAccess(storeId)) {
            throw new IllegalArgumentException("No access to store: " + storeId);
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found."));

        if (!"PENDING_CLEAN".equals(table.getTableStatus())) {
            throw new IllegalStateException(
                    "Table must be PENDING_CLEAN to mark clean, current: " + table.getTableStatus());
        }

        table.setTableStatus("AVAILABLE");
        storeTableRepository.saveAndFlush(table);

        QrTokenResultDto qrResult = qrTokenService.refreshQr(storeId, tableId);

        return new TableCleanResultDto("AVAILABLE", qrResult.token());
    }
}
