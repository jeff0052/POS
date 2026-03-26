package com.developer.pos.v2.gto.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.gto.application.dto.GenerateGtoBatchCommand;
import com.developer.pos.v2.gto.application.dto.GtoExportBatchDto;
import com.developer.pos.v2.gto.infrastructure.persistence.entity.GtoExportBatchEntity;
import com.developer.pos.v2.gto.infrastructure.persistence.entity.GtoExportItemEntity;
import com.developer.pos.v2.gto.infrastructure.persistence.repository.GtoSettlementQueryRepository;
import com.developer.pos.v2.gto.infrastructure.persistence.repository.JpaGtoExportBatchRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GtoExportApplicationService implements UseCase {

    private final JpaGtoExportBatchRepository batchRepository;
    private final GtoSettlementQueryRepository settlementQueryRepository;

    public GtoExportApplicationService(
            JpaGtoExportBatchRepository batchRepository,
            GtoSettlementQueryRepository settlementQueryRepository
    ) {
        this.batchRepository = batchRepository;
        this.settlementQueryRepository = settlementQueryRepository;
    }

    @Transactional
    public GtoExportBatchDto generateBatch(GenerateGtoBatchCommand command) {
        validateCommand(command);

        batchRepository.findByStoreIdAndExportDate(command.storeId(), command.exportDate())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "GTO batch already exists for store " + command.storeId()
                                    + " on " + command.exportDate() + " (batchId=" + existing.getBatchId() + ")");
                });

        GtoExportBatchEntity batch = new GtoExportBatchEntity();
        batch.setBatchId(UUID.randomUUID().toString());
        batch.setMerchantId(command.merchantId());
        batch.setStoreId(command.storeId());
        batch.setExportDate(command.exportDate());
        batch.setBatchStatus("GENERATING");
        batch.setCreatedAt(LocalDateTime.now());

        try {
            List<SettlementRecordEntity> settlements = settlementQueryRepository
                    .findSettledByStoreIdAndDate(command.storeId(), command.exportDate().toString());

            Map<String, List<SettlementRecordEntity>> grouped = settlements.stream()
                    .collect(Collectors.groupingBy(SettlementRecordEntity::getPaymentMethod));

            long batchTotalSales = 0;
            long batchTotalTax = 0;
            int batchTotalCount = 0;

            for (Map.Entry<String, List<SettlementRecordEntity>> entry : grouped.entrySet()) {
                String paymentMethod = entry.getKey();
                List<SettlementRecordEntity> records = entry.getValue();

                long saleTotalCents = 0;
                int saleCount = 0;
                long refundTotalCents = 0;
                int refundCount = 0;

                for (SettlementRecordEntity record : records) {
                    long amount = record.getCollectedAmountCents();
                    if (amount >= 0) {
                        saleTotalCents += amount;
                        saleCount++;
                    } else {
                        refundTotalCents += Math.abs(amount);
                        refundCount++;
                    }
                }

                long netTotalCents = saleTotalCents - refundTotalCents;
                long taxCents = (netTotalCents * 9) / 109;

                GtoExportItemEntity item = new GtoExportItemEntity();
                item.setBatch(batch);
                item.setPaymentMethod(paymentMethod);
                item.setSaleCount(saleCount);
                item.setSaleTotalCents(saleTotalCents);
                item.setRefundCount(refundCount);
                item.setRefundTotalCents(refundTotalCents);
                item.setNetTotalCents(netTotalCents);
                item.setTaxCents(taxCents);

                batch.getItems().add(item);

                batchTotalSales += netTotalCents;
                batchTotalTax += taxCents;
                batchTotalCount += saleCount + refundCount;
            }

            batch.setTotalSalesCents(batchTotalSales);
            batch.setTotalTaxCents(batchTotalTax);
            batch.setTotalTransactionCount(batchTotalCount);
            batch.setBatchStatus("COMPLETED");
            batch.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            batch.setBatchStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
        }

        GtoExportBatchEntity saved = batchRepository.save(batch);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public GtoExportBatchDto getBatch(String batchId) {
        GtoExportBatchEntity batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new IllegalArgumentException("GTO batch not found: " + batchId));
        return toDto(batch);
    }

    @Transactional(readOnly = true)
    public Page<GtoExportBatchDto> listBatches(Long merchantId, int page, int size) {
        Page<GtoExportBatchEntity> batchPage = batchRepository
                .findByMerchantIdOrderByExportDateDesc(merchantId, PageRequest.of(page, size));
        return batchPage.map(this::toDto);
    }

    @Transactional
    public GtoExportBatchDto retryBatch(String batchId) {
        GtoExportBatchEntity existing = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new IllegalArgumentException("GTO batch not found: " + batchId));

        if (!"FAILED".equals(existing.getBatchStatus())) {
            throw new IllegalStateException("Only FAILED batches can be retried. Current status: " + existing.getBatchStatus());
        }

        batchRepository.delete(existing);
        batchRepository.flush();

        return generateBatch(new GenerateGtoBatchCommand(
                existing.getMerchantId(),
                existing.getStoreId(),
                existing.getExportDate()
        ));
    }

    private GtoExportBatchDto toDto(GtoExportBatchEntity batch) {
        List<GtoExportBatchDto.GtoExportItemDto> itemDtos = batch.getItems().stream()
                .map(item -> new GtoExportBatchDto.GtoExportItemDto(
                        item.getPaymentMethod(),
                        item.getPaymentScheme(),
                        item.getSaleCount(),
                        item.getSaleTotalCents(),
                        item.getRefundCount(),
                        item.getRefundTotalCents(),
                        item.getNetTotalCents(),
                        item.getTaxCents()
                ))
                .toList();

        return new GtoExportBatchDto(
                batch.getBatchId(),
                batch.getMerchantId(),
                batch.getStoreId(),
                batch.getExportDate(),
                batch.getBatchStatus(),
                batch.getTotalSalesCents(),
                batch.getTotalTaxCents(),
                batch.getTotalTransactionCount(),
                batch.getErrorMessage(),
                batch.getCreatedAt(),
                batch.getCompletedAt(),
                itemDtos
        );
    }

    private void validateCommand(GenerateGtoBatchCommand command) {
        if (command.merchantId() == null) {
            throw new IllegalArgumentException("merchantId must not be null.");
        }
        if (command.storeId() == null) {
            throw new IllegalArgumentException("storeId must not be null.");
        }
        if (command.exportDate() == null) {
            throw new IllegalArgumentException("exportDate must not be null.");
        }
        if (command.exportDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("exportDate must not be in the future.");
        }
    }
}
