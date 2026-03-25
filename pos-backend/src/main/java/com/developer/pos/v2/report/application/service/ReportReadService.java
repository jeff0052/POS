package com.developer.pos.v2.report.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberRechargeOrderEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRechargeOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.report.application.dto.V2DailySummaryDto;
import com.developer.pos.v2.report.application.dto.V2SalesReportSummaryDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportReadService {

    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaMemberRechargeOrderRepository memberRechargeOrderRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public ReportReadService(
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaMemberRechargeOrderRepository memberRechargeOrderRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.settlementRecordRepository = settlementRecordRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.memberRechargeOrderRepository = memberRechargeOrderRepository;
        this.storeTableRepository = storeTableRepository;
    }

    public V2DailySummaryDto getDailySummary(Long storeId) {
        List<SettlementRecordEntity> settlements = settlementRecordRepository.findAll()
                .stream()
                .filter(item -> item.getStoreId().equals(storeId))
                .toList();

        long totalRevenue = settlements.stream().mapToLong(SettlementRecordEntity::getCollectedAmountCents).sum();
        long cashAmount = settlements.stream()
                .filter(item -> "CASH".equals(item.getPaymentMethod()))
                .mapToLong(SettlementRecordEntity::getCollectedAmountCents)
                .sum();
        long sdkPayAmount = settlements.stream()
                .filter(item -> "SDK_PAY".equals(item.getPaymentMethod()))
                .mapToLong(SettlementRecordEntity::getCollectedAmountCents)
                .sum();

        return new V2DailySummaryDto(
                totalRevenue,
                settlements.size(),
                0,
                cashAmount,
                sdkPayAmount
        );
    }

    public V2SalesReportSummaryDto getSalesSummary(Long storeId, Long merchantId) {
        List<ActiveTableOrderEntity> activeOrders = activeTableOrderRepository.findAllByStoreIdOrderByIdDesc(storeId);
        long totalSales = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getPayableAmountCents).sum();
        long memberSales = activeOrders.stream()
                .filter(item -> item.getMemberId() != null)
                .mapToLong(ActiveTableOrderEntity::getPayableAmountCents)
                .sum();
        long memberDiscounts = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getMemberDiscountCents).sum();
        long promotionDiscounts = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getPromotionDiscountCents).sum();
        long rechargeSales = memberRechargeOrderRepository.findAllByMerchantIdOrderByIdDesc(merchantId)
                .stream()
                .mapToLong(MemberRechargeOrderEntity::getAmountCents)
                .sum();
        long totalDiscount = memberDiscounts + promotionDiscounts;
        long settledCount = settlementRecordRepository.findAll()
                .stream()
                .filter(item -> item.getStoreId().equals(storeId))
                .count();
        long tableCount = storeTableRepository.findAll().stream().filter(item -> item.getStoreId().equals(storeId)).count();
        double turnover = tableCount == 0 ? 0 : (double) settledCount / (double) tableCount;

        return new V2SalesReportSummaryDto(
                totalSales,
                totalDiscount,
                memberSales,
                rechargeSales,
                turnover,
                0
        );
    }
}
