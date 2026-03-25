package com.developer.pos.v2.report.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberRechargeOrderEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRechargeOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.report.application.dto.V2DailySummaryDto;
import com.developer.pos.v2.report.application.dto.OrderStateIssueDto;
import com.developer.pos.v2.report.application.dto.OrderStateMonitorDto;
import com.developer.pos.v2.report.application.dto.V2SalesReportSummaryDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportReadService {

    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaMemberRechargeOrderRepository memberRechargeOrderRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public ReportReadService(
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSubmittedOrderRepository submittedOrderRepository,
            JpaTableSessionRepository tableSessionRepository,
            JpaMemberRechargeOrderRepository memberRechargeOrderRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.settlementRecordRepository = settlementRecordRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.submittedOrderRepository = submittedOrderRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.memberRechargeOrderRepository = memberRechargeOrderRepository;
        this.storeTableRepository = storeTableRepository;
    }

    public V2DailySummaryDto getDailySummary(Long storeId) {
        List<SettlementRecordEntity> settlements = safeSettlementRecords()
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
        List<ActiveTableOrderEntity> activeOrders = safeActiveOrders(storeId);
        long totalSales = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getPayableAmountCents).sum();
        long memberSales = activeOrders.stream()
                .filter(item -> item.getMemberId() != null)
                .mapToLong(ActiveTableOrderEntity::getPayableAmountCents)
                .sum();
        long memberDiscounts = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getMemberDiscountCents).sum();
        long promotionDiscounts = activeOrders.stream().mapToLong(ActiveTableOrderEntity::getPromotionDiscountCents).sum();
        long rechargeSales = safeRechargeOrders(merchantId)
                .stream()
                .mapToLong(MemberRechargeOrderEntity::getAmountCents)
                .sum();
        long totalDiscount = memberDiscounts + promotionDiscounts;
        long settledCount = safeSettlementRecords()
                .stream()
                .filter(item -> item.getStoreId().equals(storeId))
                .count();
        long tableCount = safeTables().stream().filter(item -> item.getStoreId().equals(storeId)).count();
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

    public OrderStateMonitorDto getOrderStateMonitor(Long storeId) {
        List<StoreTableEntity> tables = safeTablesByStore(storeId);
        List<ActiveTableOrderEntity> activeOrders = safeActiveOrders(storeId);
        List<TableSessionEntity> sessions = safeSessions(storeId);
        List<SubmittedOrderEntity> submittedOrders = safeSubmittedOrders(storeId);

        Map<Long, ActiveTableOrderEntity> activeOrderByTableId = new HashMap<>();
        activeOrders.forEach(item -> activeOrderByTableId.putIfAbsent(item.getTableId(), item));

        Map<Long, TableSessionEntity> openSessionByTableId = new HashMap<>();
        sessions.stream()
                .filter(item -> "OPEN".equals(item.getSessionStatus()))
                .forEach(item -> openSessionByTableId.putIfAbsent(item.getTableId(), item));

        Map<Long, Integer> unsettledSubmittedCountByTableId = new HashMap<>();
        submittedOrders.stream()
                .filter(item -> !"SETTLED".equals(item.getSettlementStatus()))
                .forEach(item -> unsettledSubmittedCountByTableId.merge(item.getTableId(), 1, Integer::sum));

        List<OrderStateIssueDto> issues = new ArrayList<>();

        for (StoreTableEntity table : tables) {
            ActiveTableOrderEntity activeOrder = activeOrderByTableId.get(table.getId());
            TableSessionEntity openSession = openSessionByTableId.get(table.getId());
            int unsettledSubmitted = unsettledSubmittedCountByTableId.getOrDefault(table.getId(), 0);
            String tableStatus = table.getTableStatus();

            if ("AVAILABLE".equals(tableStatus) && activeOrder != null) {
                issues.add(new OrderStateIssueDto(
                        "AVAILABLE_WITH_ACTIVE_ORDER",
                        "HIGH",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        activeOrder.getActiveOrderId(),
                        activeOrder.getStatus().name(),
                        openSession != null ? openSession.getSessionId() : null,
                        openSession != null ? openSession.getSessionStatus() : null,
                        unsettledSubmitted,
                        "Table is AVAILABLE but still has an active order."
                ));
            }

            if ("AVAILABLE".equals(tableStatus) && openSession != null) {
                issues.add(new OrderStateIssueDto(
                        "AVAILABLE_WITH_OPEN_SESSION",
                        "HIGH",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        activeOrder != null ? activeOrder.getActiveOrderId() : null,
                        activeOrder != null ? activeOrder.getStatus().name() : null,
                        openSession.getSessionId(),
                        openSession.getSessionStatus(),
                        unsettledSubmitted,
                        "Table is AVAILABLE but still has an open table session."
                ));
            }

            if ("AVAILABLE".equals(tableStatus) && unsettledSubmitted > 0) {
                issues.add(new OrderStateIssueDto(
                        "AVAILABLE_WITH_UNSETTLED_SUBMITTED_ORDERS",
                        "HIGH",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        activeOrder != null ? activeOrder.getActiveOrderId() : null,
                        activeOrder != null ? activeOrder.getStatus().name() : null,
                        openSession != null ? openSession.getSessionId() : null,
                        openSession != null ? openSession.getSessionStatus() : null,
                        unsettledSubmitted,
                        "Table is AVAILABLE but still has unsettled submitted orders."
                ));
            }

            if ("ORDERING".equals(tableStatus) && activeOrder == null) {
                issues.add(new OrderStateIssueDto(
                        "ORDERING_WITHOUT_ACTIVE_ORDER",
                        "MEDIUM",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        null,
                        null,
                        openSession != null ? openSession.getSessionId() : null,
                        openSession != null ? openSession.getSessionStatus() : null,
                        unsettledSubmitted,
                        "Table is ORDERING but no active draft order was found."
                ));
            }

            if ("DINING".equals(tableStatus) && unsettledSubmitted == 0) {
                issues.add(new OrderStateIssueDto(
                        "DINING_WITHOUT_SUBMITTED_ORDERS",
                        "MEDIUM",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        activeOrder != null ? activeOrder.getActiveOrderId() : null,
                        activeOrder != null ? activeOrder.getStatus().name() : null,
                        openSession != null ? openSession.getSessionId() : null,
                        openSession != null ? openSession.getSessionStatus() : null,
                        unsettledSubmitted,
                        "Table is DINING but no unsettled submitted orders were found."
                ));
            }

            if ("PENDING_SETTLEMENT".equals(tableStatus) && unsettledSubmitted == 0) {
                issues.add(new OrderStateIssueDto(
                        "PAYMENT_PENDING_WITHOUT_SUBMITTED_ORDERS",
                        "HIGH",
                        table.getId(),
                        table.getTableCode(),
                        tableStatus,
                        activeOrder != null ? activeOrder.getActiveOrderId() : null,
                        activeOrder != null ? activeOrder.getStatus().name() : null,
                        openSession != null ? openSession.getSessionId() : null,
                        openSession != null ? openSession.getSessionStatus() : null,
                        unsettledSubmitted,
                        "Table is PENDING_SETTLEMENT but no unsettled submitted orders were found."
                ));
            }
        }

        long availableCount = tables.stream().filter(item -> "AVAILABLE".equals(item.getTableStatus())).count();
        long orderingCount = tables.stream().filter(item -> "ORDERING".equals(item.getTableStatus())).count();
        long diningCount = tables.stream().filter(item -> "DINING".equals(item.getTableStatus())).count();
        long paymentPendingCount = tables.stream().filter(item -> "PENDING_SETTLEMENT".equals(item.getTableStatus())).count();
        long cleaningCount = tables.stream().filter(item -> "CLEANING".equals(item.getTableStatus())).count();
        long openSessions = sessions.stream().filter(item -> "OPEN".equals(item.getSessionStatus())).count();
        long activeDrafts = activeOrders.stream().filter(item -> "DRAFT".equals(item.getStatus().name())).count();
        long activeSubmitted = activeOrders.stream().filter(item -> "SUBMITTED".equals(item.getStatus().name())).count();
        long unsettledSubmittedOrders = submittedOrders.stream().filter(item -> !"SETTLED".equals(item.getSettlementStatus())).count();

        return new OrderStateMonitorDto(
                storeId,
                tables.size(),
                availableCount,
                orderingCount,
                diningCount,
                paymentPendingCount,
                cleaningCount,
                openSessions,
                activeDrafts,
                activeSubmitted,
                unsettledSubmittedOrders,
                issues.size(),
                issues
        );
    }

    private List<SettlementRecordEntity> safeSettlementRecords() {
        try {
            return settlementRecordRepository.findAll();
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<ActiveTableOrderEntity> safeActiveOrders(Long storeId) {
        try {
            return activeTableOrderRepository.findAllByStoreIdOrderByIdDesc(storeId);
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<MemberRechargeOrderEntity> safeRechargeOrders(Long merchantId) {
        try {
            return memberRechargeOrderRepository.findAllByMerchantIdOrderByIdDesc(merchantId);
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<StoreTableEntity> safeTables() {
        try {
            return storeTableRepository.findAll();
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<StoreTableEntity> safeTablesByStore(Long storeId) {
        try {
            return storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId);
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<TableSessionEntity> safeSessions(Long storeId) {
        try {
            return tableSessionRepository.findAllByStoreIdOrderByIdDesc(storeId);
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private List<SubmittedOrderEntity> safeSubmittedOrders(Long storeId) {
        try {
            return submittedOrderRepository.findAllByStoreIdOrderByIdDesc(storeId);
        } catch (DataAccessException ignored) {
            return Collections.emptyList();
        }
    }
}
