package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TableSettlementFinalizer {

    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public TableSettlementFinalizer(
            JpaSubmittedOrderRepository submittedOrderRepository,
            JpaTableSessionRepository tableSessionRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaStoreTableRepository storeTableRepository) {
        this.submittedOrderRepository = submittedOrderRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.storeTableRepository = storeTableRepository;
    }

    /**
     * 结算闭环：submitted_orders → SETTLED, sessions → CLOSED, tables → PENDING_CLEAN, 删 active orders.
     * 由 collectForTable（旧路径）和 confirmStacking（新路径）共同调用。
     *
     * @param sessionChainIds masterSessionId + all mergedSessionIds
     */
    @Transactional
    public void finalize(List<Long> sessionChainIds) {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. submitted_orders UNPAID → SETTLED
        List<SubmittedOrderEntity> orders = submittedOrderRepository
                .findByTableSessionIdInAndSettlementStatusOrderByIdAsc(sessionChainIds, "UNPAID");
        for (SubmittedOrderEntity o : orders) {
            o.setSettlementStatus("SETTLED");
            o.setSettledAt(now);
        }
        submittedOrderRepository.saveAll(orders);

        // 2. sessions → CLOSED
        List<TableSessionEntity> sessions = tableSessionRepository.findAllById(sessionChainIds);
        for (TableSessionEntity s : sessions) {
            s.setSessionStatus("CLOSED");
            s.setClosedAt(now);
            if (s.getMergedIntoSessionId() != null) {
                s.setMergedIntoSessionId(null);
            }
        }
        tableSessionRepository.saveAll(sessions);

        // 3. tables → PENDING_CLEAN
        List<Long> tableIds = sessions.stream()
                .map(TableSessionEntity::getTableId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<StoreTableEntity> tables = storeTableRepository.findAllById(tableIds);
        for (StoreTableEntity t : tables) {
            t.setTableStatus("PENDING_CLEAN");
        }
        storeTableRepository.saveAll(tables);

        // 4. 删 active orders (active_table_orders has no tableSessionId column; delete per storeId+tableId)
        for (TableSessionEntity s : sessions) {
            activeTableOrderRepository.findByStoreIdAndTableId(s.getStoreId(), s.getTableId())
                    .ifPresent(activeTableOrderRepository::delete);
        }
    }
}
