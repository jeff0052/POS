package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundRecordRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RefundApplicationService implements UseCase {

    private final JpaRefundRecordRepository refundRecordRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;

    public RefundApplicationService(
            JpaRefundRecordRepository refundRecordRepository,
            JpaSettlementRecordRepository settlementRecordRepository
    ) {
        this.refundRecordRepository = refundRecordRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Transactional
    public RefundRecordDto createRefund(CreateRefundCommand command) {
        SettlementRecordEntity settlement = settlementRecordRepository.findByIdForUpdate(command.settlementId())
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + command.settlementId()));

        if (!"SETTLED".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Can only refund SETTLED orders. Current: " + settlement.getFinalStatus());
        }

        long alreadyRefunded = settlement.getRefundedAmountCents();
        long maxRefundable = settlement.getCollectedAmountCents() - alreadyRefunded;

        String refundType = command.refundType() == null ? "FULL" : command.refundType().toUpperCase();
        long refundAmount;

        if ("FULL".equals(refundType)) {
            refundAmount = maxRefundable;
        } else {
            refundAmount = command.refundAmountCents();
        }

        if (refundAmount <= 0) {
            throw new IllegalStateException("Nothing to refund. Already fully refunded.");
        }

        if (refundAmount > maxRefundable) {
            throw new IllegalStateException(
                    "Refund amount " + refundAmount + " exceeds refundable " + maxRefundable +
                    " (collected=" + settlement.getCollectedAmountCents() + ", already refunded=" + alreadyRefunded + ")");
        }

        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        refund.setSettlementId(settlement.getId());
        refund.setSettlementNo(settlement.getSettlementNo());
        refund.setMerchantId(settlement.getMerchantId());
        refund.setStoreId(settlement.getStoreId());
        refund.setRefundAmountCents(refundAmount);
        refund.setRefundType(refundType);
        refund.setRefundReason(command.refundReason());
        refund.setRefundStatus("COMPLETED");
        refund.setPaymentMethod(settlement.getPaymentMethod());
        refund.setOperatedBy(command.operatedBy());

        refundRecordRepository.save(refund);

        settlement.setRefundedAmountCents(alreadyRefunded + refundAmount);
        if (settlement.getRefundedAmountCents() >= settlement.getCollectedAmountCents()) {
            settlement.setRefundStatus("FULLY_REFUNDED");
        } else {
            settlement.setRefundStatus("PARTIALLY_REFUNDED");
        }
        settlementRecordRepository.save(settlement);

        return toDto(refund);
    }

    @Transactional(readOnly = true)
    public RefundRecordDto getRefund(String refundNo) {
        return refundRecordRepository.findByRefundNo(refundNo)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundNo));
    }

    @Transactional(readOnly = true)
    public List<RefundRecordDto> getRefundsBySettlement(Long settlementId) {
        return refundRecordRepository.findBySettlementId(settlementId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RefundRecordDto> listRefunds(Long storeId, int page, int size) {
        return refundRecordRepository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(page, size))
                .map(this::toDto);
    }

    private RefundRecordDto toDto(RefundRecordEntity entity) {
        return new RefundRecordDto(
                entity.getId(),
                entity.getRefundNo(),
                entity.getSettlementId(),
                entity.getSettlementNo(),
                entity.getMerchantId(),
                entity.getStoreId(),
                entity.getRefundAmountCents(),
                entity.getRefundType(),
                entity.getRefundReason(),
                entity.getRefundStatus(),
                entity.getPaymentMethod(),
                entity.getOperatedBy(),
                entity.getApprovedBy(),
                entity.getCreatedAt()
        );
    }
}
