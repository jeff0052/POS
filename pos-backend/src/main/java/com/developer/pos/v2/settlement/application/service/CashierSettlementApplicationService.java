package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CashierSettlementApplicationService implements UseCase {

    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public CashierSettlementApplicationService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.storeTableRepository = storeTableRepository;
    }

    @Transactional
    public CashierSettlementResultDto collect(CollectCashierSettlementCommand command) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(command.activeOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + command.activeOrderId()));

        if (activeOrder.getStatus() != ActiveOrderStatus.PENDING_SETTLEMENT) {
            throw new IllegalStateException("Only pending settlement orders can be collected.");
        }

        SettlementRecordEntity settlementRecord = new SettlementRecordEntity();
        settlementRecord.setSettlementNo("SET" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        settlementRecord.setActiveOrderId(activeOrder.getActiveOrderId());
        settlementRecord.setMerchantId(activeOrder.getMerchantId());
        settlementRecord.setStoreId(activeOrder.getStoreId());
        settlementRecord.setTableId(activeOrder.getTableId());
        settlementRecord.setCashierId(command.cashierId());
        settlementRecord.setPaymentMethod(command.paymentMethod());
        settlementRecord.setFinalStatus("SETTLED");
        settlementRecord.setPayableAmountCents(activeOrder.getPayableAmountCents());
        settlementRecord.setCollectedAmountCents(command.collectedAmountCents());
        settlementRecordRepository.save(settlementRecord);

        activeOrder.setStatus(ActiveOrderStatus.SETTLED);
        activeTableOrderRepository.save(activeOrder);

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(activeOrder.getTableId(), activeOrder.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Store table not found for settlement."));
        table.setTableStatus("AVAILABLE");
        storeTableRepository.save(table);

        return new CashierSettlementResultDto(
                activeOrder.getActiveOrderId(),
                settlementRecord.getSettlementNo(),
                activeOrder.getStatus().name(),
                activeOrder.getPayableAmountCents(),
                command.collectedAmountCents()
        );
    }
}
