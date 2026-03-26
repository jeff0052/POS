package com.developer.pos.v2.staff.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.staff.application.dto.ShiftClosedDto;
import com.developer.pos.v2.staff.application.dto.ShiftOpenedDto;
import com.developer.pos.v2.staff.application.dto.ShiftSummaryDto;
import com.developer.pos.v2.staff.infrastructure.persistence.entity.CashierShiftEntity;
import com.developer.pos.v2.staff.infrastructure.persistence.repository.JpaCashierShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ShiftApplicationService implements UseCase {

    private final JpaCashierShiftRepository cashierShiftRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;

    public ShiftApplicationService(
            JpaCashierShiftRepository cashierShiftRepository,
            JpaSettlementRecordRepository settlementRecordRepository
    ) {
        this.cashierShiftRepository = cashierShiftRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Transactional
    public ShiftOpenedDto openShift(Long storeId, Long cashierId, String cashierName, long openingFloatCents) {
        cashierShiftRepository.findFirstByStoreIdAndCashierIdAndShiftStatusOrderByIdDesc(storeId, cashierId, "OPEN")
                .ifPresent(existing -> {
                    throw new IllegalStateException("An open shift already exists for this cashier.");
                });

        CashierShiftEntity shift = new CashierShiftEntity();
        shift.setShiftId("SHIFT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        shift.setStoreId(storeId);
        shift.setCashierId(cashierId);
        shift.setCashierName(cashierName == null ? "" : cashierName.trim());
        shift.setShiftStatus("OPEN");
        shift.setOpeningFloatCents(openingFloatCents);
        CashierShiftEntity saved = cashierShiftRepository.saveAndFlush(shift);

        return new ShiftOpenedDto(
                saved.getShiftId(),
                saved.getStoreId(),
                saved.getCashierId(),
                saved.getCashierName(),
                saved.getShiftStatus(),
                saved.getOpeningFloatCents(),
                saved.getOpenedAt()
        );
    }

    @Transactional
    public ShiftClosedDto closeShift(Long storeId, String shiftId, long closingCashCents, String closingNote) {
        CashierShiftEntity shift = cashierShiftRepository.findByShiftIdAndStoreId(shiftId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        if (!"OPEN".equalsIgnoreCase(shift.getShiftStatus())) {
            throw new IllegalStateException("Only open shifts can be closed.");
        }

        shift.setShiftStatus("CLOSED");
        shift.setClosingCashCents(closingCashCents);
        shift.setClosingNote(closingNote == null ? null : closingNote.trim());
        shift.setClosedAt(OffsetDateTime.now());
        CashierShiftEntity saved = cashierShiftRepository.saveAndFlush(shift);

        return new ShiftClosedDto(
                saved.getShiftId(),
                saved.getShiftStatus(),
                saved.getClosingCashCents() == null ? 0 : saved.getClosingCashCents(),
                saved.getClosingNote(),
                saved.getClosedAt()
        );
    }

    @Transactional(readOnly = true)
    public ShiftSummaryDto getShiftSummary(Long storeId, String shiftId) {
        CashierShiftEntity shift = cashierShiftRepository.findByShiftIdAndStoreId(shiftId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

        OffsetDateTime from = shift.getOpenedAt();
        OffsetDateTime to = shift.getClosedAt() == null ? OffsetDateTime.now() : shift.getClosedAt();
        List<SettlementRecordEntity> settlements = settlementRecordRepository
                .findAllByStoreIdAndCashierIdAndCreatedAtBetweenOrderByIdAsc(storeId, shift.getCashierId(), from, to);

        long payableAmount = settlements.stream().mapToLong(SettlementRecordEntity::getPayableAmountCents).sum();
        long collectedAmount = settlements.stream().mapToLong(SettlementRecordEntity::getCollectedAmountCents).sum();

        return new ShiftSummaryDto(
                shift.getShiftId(),
                shift.getStoreId(),
                shift.getCashierId(),
                shift.getCashierName(),
                shift.getShiftStatus(),
                shift.getOpeningFloatCents(),
                shift.getClosingCashCents(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                settlements.size(),
                payableAmount,
                collectedAmount
        );
    }
}
