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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CashierSettlementApplicationService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaMemberRepository memberRepository,
            JpaPromotionHitRepository promotionHitRepository,
            ObjectMapper objectMapper,
            JpaTableSessionRepository tableSessionRepository,
            JpaSubmittedOrderRepository submittedOrderRepository
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.storeTableRepository = storeTableRepository;
        this.memberRepository = memberRepository;
        this.promotionHitRepository = promotionHitRepository;
        this.objectMapper = objectMapper;
        this.tableSessionRepository = tableSessionRepository;
        this.submittedOrderRepository = submittedOrderRepository;
    }

    @Transactional(readOnly = true)
    public SettlementPreviewDto getSettlementPreview(String activeOrderId) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

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

        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(session.getId(), "UNPAID");
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

        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(session.getId(), "UNPAID");
        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("No submitted orders are ready for payment.");
        }

        ActiveTableOrderEntity draftOrder = activeTableOrderRepository.findByStoreIdAndTableId(storeId, tableId).orElse(null);
        if (draftOrder != null && draftOrder.getStatus() == ActiveOrderStatus.DRAFT && !draftOrder.getItems().isEmpty()) {
            throw new IllegalStateException("Current draft must be sent to kitchen before payment.");
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalStateException("Store table not found for payment pending."));
        table.setTableStatus("PENDING_SETTLEMENT");
        storeTableRepository.saveAndFlush(table);

        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByStoreIdAndTableId(storeId, tableId).orElse(null);
        if (activeOrder != null && activeOrder.getStatus() == ActiveOrderStatus.SUBMITTED) {
            activeOrder.setStatus(ActiveOrderStatus.PENDING_SETTLEMENT);
            activeTableOrderRepository.saveAndFlush(activeOrder);
        }

        return new OrderStageTransitionDto(session.getSessionId(), ActiveOrderStatus.PENDING_SETTLEMENT.name());
    }

    @Transactional
    public CashierSettlementResultDto collectForTable(Long storeId, Long tableId, CollectCashierSettlementCommand command) {
        TableSessionEntity session = tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("Open table session not found."));

        List<SubmittedOrderEntity> unpaidOrders = submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(session.getId(), "UNPAID");
        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("No unpaid submitted orders found for collection.");
        }

        long payableAmount = unpaidOrders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();

        SettlementRecordEntity settlementRecord = new SettlementRecordEntity();
        settlementRecord.setSettlementNo("SET" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        settlementRecord.setActiveOrderId(session.getSessionId());
        settlementRecord.setMerchantId(unpaidOrders.get(0).getMerchantId());
        settlementRecord.setStoreId(storeId);
        settlementRecord.setTableId(tableId);
        settlementRecord.setCashierId(command.cashierId());
        settlementRecord.setPaymentMethod(command.paymentMethod());
        settlementRecord.setFinalStatus("SETTLED");
        settlementRecord.setPayableAmountCents(payableAmount);
        settlementRecord.setCollectedAmountCents(command.collectedAmountCents());
        settlementRecordRepository.saveAndFlush(settlementRecord);

        unpaidOrders.forEach(submittedOrder -> {
            submittedOrder.setSettlementStatus("SETTLED");
            submittedOrder.setSettledAt(OffsetDateTime.now());
        });
        submittedOrderRepository.saveAllAndFlush(unpaidOrders);

        session.setSessionStatus("CLOSED");
        session.setClosedAt(OffsetDateTime.now());
        tableSessionRepository.saveAndFlush(session);

        ActiveTableOrderEntity currentActiveOrder = activeTableOrderRepository.findByStoreIdAndTableId(storeId, tableId).orElse(null);
        if (currentActiveOrder != null) {
            activeTableOrderRepository.delete(currentActiveOrder);
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalStateException("Store table not found for settlement."));
        table.setTableStatus("AVAILABLE");
        storeTableRepository.saveAndFlush(table);

        return new CashierSettlementResultDto(
                session.getSessionId(),
                settlementRecord.getSettlementNo(),
                ActiveOrderStatus.SETTLED.name(),
                payableAmount,
                command.collectedAmountCents()
        );
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
        settlementRecordRepository.saveAndFlush(settlementRecord);

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
        table.setTableStatus("AVAILABLE");
        storeTableRepository.saveAndFlush(table);

        return new CashierSettlementResultDto(
                activeOrder.getActiveOrderId(),
                settlementRecord.getSettlementNo(),
                activeOrder.getStatus().name(),
                activeOrder.getPayableAmountCents(),
                command.collectedAmountCents()
        );
    }

    private SettlementPreviewDto.GiftItemDto readGiftItem(String snapshot) {
        try {
            PromotionEvaluationDto.GiftItemDto giftItem = objectMapper.readValue(snapshot, PromotionEvaluationDto.GiftItemDto.class);
            return new SettlementPreviewDto.GiftItemDto(giftItem.skuName(), giftItem.quantity());
        } catch (Exception exception) {
            return new SettlementPreviewDto.GiftItemDto(snapshot, 1);
        }
    }
}
