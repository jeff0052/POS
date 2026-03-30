package com.developer.pos.v2.channel.application.service;

import com.developer.pos.v2.channel.application.dto.ChannelSettlementBatchDto;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRecordEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelSettlementBatchEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRecordRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelSettlementBatchRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChannelSettlementService implements UseCase {

    private final JpaChannelCommissionRecordRepository commissionRecordRepository;
    private final JpaChannelSettlementBatchRepository settlementBatchRepository;

    public ChannelSettlementService(
            JpaChannelCommissionRecordRepository commissionRecordRepository,
            JpaChannelSettlementBatchRepository settlementBatchRepository
    ) {
        this.commissionRecordRepository = commissionRecordRepository;
        this.settlementBatchRepository = settlementBatchRepository;
    }

    @Transactional
    public ChannelSettlementBatchDto generateBatch(Long channelId, LocalDate periodStart, LocalDate periodEnd) {
        List<ChannelCommissionRecordEntity> pendingRecords =
                commissionRecordRepository.findByChannelIdAndCommissionStatusOrderByCreatedAtDesc(channelId, "PENDING");

        // Filter records within the period
        List<ChannelCommissionRecordEntity> periodRecords = pendingRecords.stream()
                .filter(r -> {
                    LocalDate recordDate = r.getCreatedAt().toLocalDate();
                    return !recordDate.isBefore(periodStart) && !recordDate.isAfter(periodEnd);
                })
                .toList();

        int totalOrders = periodRecords.size();
        long totalOrderAmountCents = periodRecords.stream()
                .mapToLong(ChannelCommissionRecordEntity::getOrderAmountCents)
                .sum();
        long totalCommissionCents = periodRecords.stream()
                .mapToLong(ChannelCommissionRecordEntity::getCommissionAmountCents)
                .sum();

        String batchNo = "CSB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();

        ChannelSettlementBatchEntity batch = new ChannelSettlementBatchEntity();
        batch.setBatchNo(batchNo);
        batch.setChannelId(channelId);
        batch.setPeriodStart(periodStart);
        batch.setPeriodEnd(periodEnd);
        batch.setTotalOrders(totalOrders);
        batch.setTotalOrderAmountCents(totalOrderAmountCents);
        batch.setTotalCommissionCents(totalCommissionCents);
        batch.setAdjustmentCents(0);
        batch.setFinalSettlementCents(totalCommissionCents);
        batch.setBatchStatus("DRAFT");
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);

        ChannelSettlementBatchEntity saved = settlementBatchRepository.save(batch);

        // Link commission records to this batch
        for (ChannelCommissionRecordEntity record : periodRecords) {
            record.setSettlementBatchId(saved.getId());
            commissionRecordRepository.save(record);
        }

        return toDto(saved);
    }

    private ChannelSettlementBatchDto toDto(ChannelSettlementBatchEntity e) {
        return new ChannelSettlementBatchDto(
                e.getId(), e.getBatchNo(), e.getChannelId(), e.getStoreId(),
                e.getPeriodStart(), e.getPeriodEnd(), e.getTotalOrders(),
                e.getTotalOrderAmountCents(), e.getTotalCommissionCents(),
                e.getAdjustmentCents(), e.getAdjustmentReason(),
                e.getFinalSettlementCents(), e.getBatchStatus(),
                e.getConfirmedAt(), e.getConfirmedBy(),
                e.getPaidAt(), e.getPaidBy(), e.getPaymentRef(), e.getNotes(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
