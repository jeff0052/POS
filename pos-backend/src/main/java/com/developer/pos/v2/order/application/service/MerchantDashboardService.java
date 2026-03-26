package com.developer.pos.v2.order.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.application.dto.MerchantDashboardDto;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class MerchantDashboardService implements UseCase {

    private static final ZoneId SGT = ZoneId.of("Asia/Singapore");

    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public MerchantDashboardService(JpaSubmittedOrderRepository submittedOrderRepository,
                                    JpaStoreTableRepository storeTableRepository) {
        this.submittedOrderRepository = submittedOrderRepository;
        this.storeTableRepository = storeTableRepository;
    }

    @Transactional(readOnly = true)
    public MerchantDashboardDto getDashboard(Long storeId) {
        List<StoreTableEntity> tables = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId);
        int totalTables = tables.size();
        int activeTables = (int) tables.stream()
                .filter(t -> !"AVAILABLE".equals(t.getTableStatus()))
                .count();

        LocalDate today = LocalDate.now(SGT);
        OffsetDateTime dayStart = today.atStartOfDay(SGT).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(SGT).toOffsetDateTime();

        List<SubmittedOrderEntity> todayOrders = submittedOrderRepository
                .findByStoreIdAndSubmittedAtBetweenOrderByIdDesc(storeId, dayStart, dayEnd);

        long todaySales = todayOrders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();
        long todayMemberDiscount = todayOrders.stream().mapToLong(SubmittedOrderEntity::getMemberDiscountCents).sum();
        long todayPromoDiscount = todayOrders.stream().mapToLong(SubmittedOrderEntity::getPromotionDiscountCents).sum();

        return new MerchantDashboardDto(
                todaySales,
                todayOrders.size(),
                activeTables,
                totalTables,
                todayMemberDiscount,
                todayPromoDiscount
        );
    }
}
