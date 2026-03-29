package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.application.dto.OrderStageTransitionDto;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.PromotionHitProjection;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.application.dto.SettlementPreviewDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Service
public class CashierSettlementApplicationService implements UseCase {

    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaPromotionHitRepository promotionHitRepository;
    private final ObjectMapper objectMapper;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final TableSettlementFinalizer tableSettlementFinalizer;

    public CashierSettlementApplicationService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaMemberRepository memberRepository,
            JpaPromotionHitRepository promotionHitRepository,
            ObjectMapper objectMapper,
            JpaTableSessionRepository tableSessionRepository,
            JpaSubmittedOrderRepository submittedOrderRepository,
            TableSettlementFinalizer tableSettlementFinalizer
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.storeTableRepository = storeTableRepository;
        this.memberRepository = memberRepository;
        this.promotionHitRepository = promotionHitRepository;
        this.objectMapper = objectMapper;
        this.tableSessionRepository = tableSessionRepository;
        this.submittedOrderRepository = submittedOrderRepository;
        this.tableSettlementFinalizer = tableSettlementFinalizer;
    }

    /**
     * Build the full session chain for a master session: [masterSessionId] + all merged session IDs.
     * Used by preview, payment-pending, and settlement to aggregate across merged tables.
     */
    /**
     * Reject settlement operations on a merged child table. Settlement must go through the master table.
     */
    private void rejectMergedChildSession(TableSessionEntity session) {
        if (session.getMergedIntoSessionId() != null) {
            throw new IllegalStateException(
                    "This table is merged into another table. Settlement must be initiated from the master table.");
        }
    }

    private List<Long> buildSessionChain(TableSessionEntity masterSession) {
        List<Long> chain = new ArrayList<>();
        chain.add(masterSession.getId());
        List<TableSessionEntity> mergedSessions = tableSessionRepository
                .findAllByMergedIntoSessionIdAndSessionStatus(masterSession.getId(), "OPEN");
        for (TableSessionEntity merged : mergedSessions) {
            chain.add(merged.getId());
        }
        return chain;
    }

    @Transactional(readOnly = true)
    public SettlementPreviewDto getSettlementPreview(String activeOrderId) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        // If this order's table has an open session, check merge status and aggregate
        TableSessionEntity session = tableSessionRepository
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(
                        activeOrder.getStoreId(), activeOrder.getTableId(), "OPEN")
                .orElse(null);

        if (session != null) {
            rejectMergedChildSession(session);

            List<Long> sessionChain = buildSessionChain(session);
            if (sessionChain.size() > 1) {
                // Merged tables exist — delegate to session-chain-aware preview,
                // but preserve the original activeOrderId so the response matches the route parameter.
                SettlementPreviewDto merged = getTableSettlementPreview(activeOrder.getStoreId(), activeOrder.getTableId());
                return new SettlementPreviewDto(
                        activeOrderId,
                        merged.status(),
                        merged.member(),
                        merged.pricing(),
                        merged.giftItems()
                );
            }
        }

        // Non-merged path: use active order's cached amounts
        SettlementPreviewDto.MemberPreviewDto member = null;
        if (activeOrder.getMemberId() != null) {
            MemberEntity memberEntity = memberRepository.findById(activeOrder.getMemberId()).orElse(null);
            if (memberEntity != null) {
                member = new SettlementPreviewDto.MemberPreviewDto(
                        memberEntity.getId(),
                        memberEntity.getName(),
                        memberEntity.getTierCode()
                );
            }
        }

        List<SettlementPreviewDto.GiftItemDto> giftItems = promotionHitRepository.findByActiveOrderDbId(activeOrder.getId()).stream()
                .map(PromotionHitProjection::getGiftSnapshotJson)
                .filter(snapshot -> snapshot != null && !snapshot.isBlank())
                .map(this::readGiftItem)
                .toList();

        return new SettlementPreviewDto(
                activeOrder.getActiveOrderId(),
                activeOrder.getStatus(),
                member,
                new SettlementPreviewDto.PricingPreviewDto(
                        activeOrder.getOriginalAmountCents(),
                        activeOrder.getMemberDiscountCents(),
                        activeOrder.getPromotionDiscountCents(),
                        activeOrder.getPayableAmountCents()
                ),
                giftItems
        );
    }

    @Transactional(readOnly = true)
    public SettlementPreviewDto getTableSettlementPreview(Long storeId, Long tableId) {
        TableSessionEntity session = tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("Open table session not found."));

        rejectMergedChildSession(session);

        List<Long> sessionChain = buildSessionChain(session);
        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository
                .findByTableSessionIdInAndSettlementStatusOrderByIdAsc(sessionChain, "UNPAID");
        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("No unpaid submitted orders found for table.");
        }

        Long memberId = unpaidOrders.stream()
                .map(SubmittedOrderEntity::getMemberId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);

        SettlementPreviewDto.MemberPreviewDto member = null;
        if (memberId != null) {
            MemberEntity memberEntity = memberRepository.findById(memberId).orElse(null);
            if (memberEntity != null) {
                member = new SettlementPreviewDto.MemberPreviewDto(
                        memberEntity.getId(),
                        memberEntity.getName(),
                        memberEntity.getTierCode()
                );
            }
        }

        long originalAmount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getOriginalAmountCents).sum();
        long memberDiscount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getMemberDiscountCents).sum();
        long promotionDiscount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getPromotionDiscountCents).sum();
        long payableAmount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();

        return new SettlementPreviewDto(
                session.getSessionId(),
                ActiveOrderStatus.PENDING_SETTLEMENT,
                member,
                new SettlementPreviewDto.PricingPreviewDto(
                        originalAmount,
                        memberDiscount,
                        promotionDiscount,
                        payableAmount
                ),
                List.of()
        );
    }

    @Transactional
    public OrderStageTransitionDto moveTableToPaymentPending(Long storeId, Long tableId) {
        TableSessionEntity session = tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("Open table session not found."));

        rejectMergedChildSession(session);

        List<Long> sessionChain = buildSessionChain(session);
        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository
                .findByTableSessionIdInAndSettlementStatusOrderByIdAsc(sessionChain, "UNPAID");
        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("No submitted orders are ready for payment.");
        }

        ActiveTableOrderEntity draftOrder = activeTableOrderRepository.findByStoreIdAndTableId(storeId, tableId).orElse(null);
        if (draftOrder != null && draftOrder.getStatus() == ActiveOrderStatus.DRAFT && !draftOrder.getItems().isEmpty()) {
            throw new IllegalStateException("Current draft must be sent to kitchen before payment.");
        }

        // Set master table to PENDING_SETTLEMENT
        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalStateException("Store table not found for payment pending."));
        table.setTableStatus("PENDING_SETTLEMENT");
        storeTableRepository.saveAndFlush(table);

        // Set merged tables to PENDING_SETTLEMENT (prevent merged tables from initiating separate settlement)
        List<TableSessionEntity> mergedSessions = tableSessionRepository
                .findAllByMergedIntoSessionIdAndSessionStatus(session.getId(), "OPEN");
        for (TableSessionEntity mergedSession : mergedSessions) {
            StoreTableEntity mergedTable = storeTableRepository.findByIdAndStoreId(mergedSession.getTableId(), storeId).orElse(null);
            if (mergedTable != null && "MERGED".equals(mergedTable.getTableStatus())) {
                mergedTable.setTableStatus("PENDING_SETTLEMENT");
                storeTableRepository.saveAndFlush(mergedTable);
            }
        }

        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByStoreIdAndTableId(storeId, tableId).orElse(null);
        if (activeOrder != null && activeOrder.getStatus() == ActiveOrderStatus.SUBMITTED) {
            activeOrder.setStatus(ActiveOrderStatus.PENDING_SETTLEMENT);
            activeTableOrderRepository.saveAndFlush(activeOrder);
        }

        return new OrderStageTransitionDto(session.getSessionId(), ActiveOrderStatus.PENDING_SETTLEMENT.name());
    }

    @Transactional
    public CashierSettlementResultDto collectForTable(Long storeId, Long tableId, CollectCashierSettlementCommand command) {
        TableSessionEntity masterSession = tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("Open table session not found."));

        rejectMergedChildSession(masterSession);

        SettlementRecordEntity existingRecord = settlementRecordRepository.findByActiveOrderId(masterSession.getSessionId()).orElse(null);
        if (existingRecord != null) {
            return toSettlementResult(masterSession.getSessionId(), existingRecord);
        }

        // D1: build session chain = [master] + all merged sessions
        List<Long> sessionChain = buildSessionChain(masterSession);
        List<TableSessionEntity> mergedSessions = tableSessionRepository
                .findAllByMergedIntoSessionIdAndSessionStatus(masterSession.getId(), "OPEN");

        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository
                .findByTableSessionIdInAndSettlementStatusOrderByIdAsc(sessionChain, "UNPAID");
        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("No unpaid submitted orders found for collection.");
        }

        long payableAmount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();

        if ("CASH".equalsIgnoreCase(command.paymentMethod())) {
            if (command.collectedAmountCents() < payableAmount) {
                throw new IllegalArgumentException(
                        "Collected amount " + command.collectedAmountCents() +
                        " is less than payable amount " + payableAmount + ". Underpayment not allowed.");
            }
        } else {
            if (command.collectedAmountCents() != payableAmount && command.collectedAmountCents() > 0) {
                throw new IllegalArgumentException(
                        "Digital payment collected amount " + command.collectedAmountCents() +
                        " does not match payable amount " + payableAmount);
            }
        }

        SettlementRecordEntity settlementRecord = new SettlementRecordEntity();
        settlementRecord.setSettlementNo("SET" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        settlementRecord.setActiveOrderId(masterSession.getSessionId());
        settlementRecord.setMerchantId(unpaidOrders.get(0).getMerchantId());
        settlementRecord.setStoreId(storeId);
        settlementRecord.setTableId(tableId);
        settlementRecord.setCashierId(command.cashierId());
        settlementRecord.setPaymentMethod(command.paymentMethod());
        settlementRecord.setFinalStatus("SETTLED");
        settlementRecord.setPayableAmountCents(payableAmount);
        settlementRecord.setCollectedAmountCents(command.collectedAmountCents());
        try {
            settlementRecordRepository.saveAndFlush(settlementRecord);
        } catch (DataIntegrityViolationException exception) {
            return settlementRecordRepository.findByActiveOrderId(masterSession.getSessionId())
                    .map(record -> toSettlementResult(masterSession.getSessionId(), record))
                    .orElseThrow(() -> exception);
        }

        // Finalize settlement: mark orders SETTLED, close sessions, set tables PENDING_CLEAN, delete active orders
        tableSettlementFinalizer.finalize(sessionChain);

        return toSettlementResult(masterSession.getSessionId(), settlementRecord);
    }

    @Transactional
    public CashierSettlementResultDto collect(CollectCashierSettlementCommand command) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(command.activeOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + command.activeOrderId()));

        SettlementRecordEntity existingRecord = settlementRecordRepository.findByActiveOrderId(activeOrder.getActiveOrderId()).orElse(null);
        if (existingRecord != null) {
            return toSettlementResult(activeOrder.getActiveOrderId(), existingRecord);
        }

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
        try {
            settlementRecordRepository.saveAndFlush(settlementRecord);
        } catch (DataIntegrityViolationException exception) {
            return settlementRecordRepository.findByActiveOrderId(activeOrder.getActiveOrderId())
                    .map(record -> toSettlementResult(activeOrder.getActiveOrderId(), record))
                    .orElseThrow(() -> exception);
        }

        activeOrder.setStatus(ActiveOrderStatus.SETTLED);
        activeTableOrderRepository.save(activeOrder);

        tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(activeOrder.getStoreId(), activeOrder.getTableId(), "OPEN")
                .ifPresent(session -> {
                    List<SubmittedOrderEntity> unpaidSubmittedOrders =
                            submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(session.getId(), "UNPAID");
                    unpaidSubmittedOrders.forEach(submittedOrder -> {
                        submittedOrder.setSettlementStatus("SETTLED");
                        submittedOrder.setSettledAt(OffsetDateTime.now());
                    });
                    submittedOrderRepository.saveAllAndFlush(unpaidSubmittedOrders);
                    session.setSessionStatus("CLOSED");
                    session.setClosedAt(OffsetDateTime.now());
                    tableSessionRepository.saveAndFlush(session);
                });

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(activeOrder.getTableId(), activeOrder.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Store table not found for settlement."));
        table.setTableStatus("PENDING_CLEAN");
        storeTableRepository.saveAndFlush(table);

        return toSettlementResult(activeOrder.getActiveOrderId(), settlementRecord);
    }

    private SettlementPreviewDto.GiftItemDto readGiftItem(String snapshot) {
        try {
            PromotionEvaluationDto.GiftItemDto giftItem = objectMapper.readValue(snapshot, PromotionEvaluationDto.GiftItemDto.class);
            return new SettlementPreviewDto.GiftItemDto(giftItem.skuName(), giftItem.quantity());
        } catch (Exception exception) {
            return new SettlementPreviewDto.GiftItemDto(snapshot, 1);
        }
    }

    private CashierSettlementResultDto toSettlementResult(String activeOrderId, SettlementRecordEntity settlementRecord) {
        return new CashierSettlementResultDto(
                activeOrderId,
                settlementRecord.getSettlementNo(),
                ActiveOrderStatus.SETTLED.name(),
                settlementRecord.getPayableAmountCents(),
                settlementRecord.getCollectedAmountCents()
        );
    }
}
