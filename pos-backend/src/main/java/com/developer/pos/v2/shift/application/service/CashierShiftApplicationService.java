package com.developer.pos.v2.shift.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.shift.application.command.CloseShiftCommand;
import com.developer.pos.v2.shift.application.command.OpenShiftCommand;
import com.developer.pos.v2.shift.application.dto.CashierShiftDto;
import com.developer.pos.v2.shift.infrastructure.persistence.entity.CashierShiftEntity;
import com.developer.pos.v2.shift.infrastructure.persistence.entity.CashierShiftSettlementEntity;
import com.developer.pos.v2.shift.infrastructure.persistence.repository.JpaCashierShiftRepository;
import com.developer.pos.v2.shift.infrastructure.persistence.repository.JpaCashierShiftSettlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CashierShiftApplicationService implements UseCase {

    private final JpaCashierShiftRepository shiftRepository;
    private final JpaCashierShiftSettlementRepository settlementRepository;

    public CashierShiftApplicationService(JpaCashierShiftRepository shiftRepository,
                                          JpaCashierShiftSettlementRepository settlementRepository) {
        this.shiftRepository = shiftRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public CashierShiftDto openShift(OpenShiftCommand command) {
        shiftRepository.findByStoreIdAndCashierStaffIdAndShiftStatus(command.storeId(), command.cashierStaffId(), "OPEN")
                .ifPresent(existing -> {
                    throw new IllegalStateException("Cashier already has an open shift: " + existing.getShiftId());
                });

        CashierShiftEntity entity = new CashierShiftEntity();
        entity.setShiftId("SH" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        entity.setMerchantId(command.merchantId());
        entity.setStoreId(command.storeId());
        entity.setCashierStaffId(command.cashierStaffId());
        entity.setCashierName(command.cashierName());
        entity.setShiftStatus("OPEN");
        entity.setOpenedAt(OffsetDateTime.now());
        entity.setOpeningCashCents(command.openingCashCents());
        CashierShiftEntity saved = shiftRepository.save(entity);
        return toDto(saved, List.of());
    }

    @Transactional
    public CashierShiftDto closeShift(CloseShiftCommand command) {
        CashierShiftEntity entity = shiftRepository.findByShiftId(command.shiftId())
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + command.shiftId()));
        if (!"OPEN".equals(entity.getShiftStatus())) {
            throw new IllegalStateException("Shift is not open: " + entity.getShiftStatus());
        }

        List<CashierShiftSettlementEntity> settlements = settlementRepository.findByShiftIdOrderBySettledAtAsc(entity.getId());
        long totalSales = settlements.stream().mapToLong(CashierShiftSettlementEntity::getAmountCents).sum();
        long cashSales = settlements.stream()
                .filter(s -> "CASH".equalsIgnoreCase(s.getPaymentMethod()))
                .mapToLong(CashierShiftSettlementEntity::getAmountCents).sum();
        long expectedCash = entity.getOpeningCashCents() + cashSales;

        entity.setShiftStatus("CLOSED");
        entity.setClosedAt(OffsetDateTime.now());
        entity.setClosingCashCents(command.closingCashCents());
        entity.setExpectedCashCents(expectedCash);
        entity.setCashDifferenceCents(command.closingCashCents() - expectedCash);
        entity.setTotalSalesCents(totalSales);
        entity.setTotalTransactionCount(settlements.size());
        entity.setNotes(command.notes());
        CashierShiftEntity saved = shiftRepository.save(entity);
        return toDto(saved, settlements);
    }

    @Transactional(readOnly = true)
    public CashierShiftDto getCurrentShift(Long storeId, String cashierStaffId) {
        CashierShiftEntity entity = shiftRepository.findByStoreIdAndCashierStaffIdAndShiftStatus(storeId, cashierStaffId, "OPEN")
                .orElse(null);
        if (entity == null) return null;
        List<CashierShiftSettlementEntity> settlements = settlementRepository.findByShiftIdOrderBySettledAtAsc(entity.getId());
        return toDto(entity, settlements);
    }

    @Transactional(readOnly = true)
    public CashierShiftDto getShiftDetail(String shiftId) {
        CashierShiftEntity entity = shiftRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
        List<CashierShiftSettlementEntity> settlements = settlementRepository.findByShiftIdOrderBySettledAtAsc(entity.getId());
        return toDto(entity, settlements);
    }

    @Transactional(readOnly = true)
    public Page<CashierShiftDto> listShifts(Long storeId, int page, int size) {
        return shiftRepository.findByStoreIdOrderByOpenedAtDesc(storeId, PageRequest.of(page, size))
                .map(entity -> toDto(entity, List.of()));
    }

    @Transactional
    public void recordSettlement(String shiftId, String settlementNo, String paymentMethod, long amountCents) {
        CashierShiftEntity shift = shiftRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));
        if (!"OPEN".equals(shift.getShiftStatus())) {
            throw new IllegalStateException("Cannot record settlement on closed shift.");
        }
        CashierShiftSettlementEntity item = new CashierShiftSettlementEntity();
        item.setShiftId(shift.getId());
        item.setSettlementNo(settlementNo);
        item.setPaymentMethod(paymentMethod);
        item.setAmountCents(amountCents);
        item.setSettledAt(OffsetDateTime.now());
        settlementRepository.save(item);
    }

    private CashierShiftDto toDto(CashierShiftEntity entity, List<CashierShiftSettlementEntity> settlements) {
        return new CashierShiftDto(
                entity.getShiftId(), entity.getStoreId(), entity.getCashierStaffId(), entity.getCashierName(),
                entity.getShiftStatus(), entity.getOpenedAt(), entity.getClosedAt(),
                entity.getOpeningCashCents(), entity.getClosingCashCents(), entity.getExpectedCashCents(),
                entity.getCashDifferenceCents(), entity.getTotalSalesCents(), entity.getTotalRefundsCents(),
                entity.getTotalTransactionCount(), entity.getNotes(),
                settlements.stream().map(s -> new CashierShiftDto.SettlementItemDto(
                        s.getSettlementNo(), s.getPaymentMethod(), s.getAmountCents(), s.getSettledAt()
                )).toList()
        );
    }
}
