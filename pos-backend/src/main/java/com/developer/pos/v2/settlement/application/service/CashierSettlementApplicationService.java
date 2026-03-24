package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.PromotionHitProjection;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.application.dto.SettlementPreviewDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CashierSettlementApplicationService implements UseCase {

    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaPromotionHitRepository promotionHitRepository;

    public CashierSettlementApplicationService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaMemberRepository memberRepository,
            JpaPromotionHitRepository promotionHitRepository
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.storeTableRepository = storeTableRepository;
        this.memberRepository = memberRepository;
        this.promotionHitRepository = promotionHitRepository;
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
                .map(snapshot -> new SettlementPreviewDto.GiftItemDto(snapshot, 1))
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
