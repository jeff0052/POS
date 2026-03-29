package com.developer.pos.v2.store.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.application.dto.ReservationDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.ReservationEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaReservationRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReservationApplicationService implements UseCase {

    private final JpaReservationRepository reservationRepository;
    private final JpaStoreRepository storeRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final StoreAccessEnforcer storeAccessEnforcer;

    public ReservationApplicationService(
            JpaReservationRepository reservationRepository,
            JpaStoreRepository storeRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaTableSessionRepository tableSessionRepository,
            StoreAccessEnforcer storeAccessEnforcer
    ) {
        this.reservationRepository = reservationRepository;
        this.storeRepository = storeRepository;
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.storeAccessEnforcer = storeAccessEnforcer;
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> listByStore(Long storeId) {
        storeAccessEnforcer.enforce(storeId);
        return reservationRepository.findAllByStoreIdOrderByIdDesc(storeId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ReservationDto create(Long storeId, String guestName, String contactPhone,
                                 String reservationTime, int partySize, String reservationStatus,
                                 String area, Long tableId) {
        storeAccessEnforcer.enforce(storeId);
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        ReservationEntity entity = new ReservationEntity();
        entity.setReservationNo("RSV" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        entity.setMerchantId(store.getMerchantId());
        entity.setStoreId(storeId);
        entity.setGuestName(guestName);
        entity.setContactPhone(contactPhone);
        entity.setReservationTime(reservationTime);
        entity.setPartySize(partySize);
        entity.setReservationStatus(reservationStatus.toUpperCase());
        entity.setArea(area);

        // Optional table pre-assignment: lock table as RESERVED
        if (tableId != null) {
            StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
            if (!"AVAILABLE".equalsIgnoreCase(table.getTableStatus())) {
                throw new IllegalStateException("Table is not available for reservation.");
            }
            table.setTableStatus("RESERVED");
            storeTableRepository.save(table);
            entity.setTableId(tableId);
        }

        return toDto(reservationRepository.save(entity));
    }

    @Transactional
    public ReservationDto update(Long storeId, Long reservationId, String guestName, String contactPhone,
                                  String reservationTime, int partySize, String reservationStatus, String area) {
        storeAccessEnforcer.enforce(storeId);
        ReservationEntity entity = reservationRepository.findByIdAndStoreId(reservationId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        String nextStatus = reservationStatus.toUpperCase();

        if ("CHECKED_IN".equalsIgnoreCase(entity.getReservationStatus())
                && entity.getTableId() != null
                && !"CHECKED_IN".equals(nextStatus)) {
            storeTableRepository.findByIdAndStoreId(entity.getTableId(), storeId).ifPresent(table -> {
                if ("RESERVED".equalsIgnoreCase(table.getTableStatus()) || "OCCUPIED".equalsIgnoreCase(table.getTableStatus())) {
                    table.setTableStatus("AVAILABLE");
                    storeTableRepository.save(table);
                }
            });
            entity.setTableId(null);
        }

        entity.setGuestName(guestName);
        entity.setContactPhone(contactPhone);
        entity.setReservationTime(reservationTime);
        entity.setPartySize(partySize);
        entity.setReservationStatus(nextStatus);
        entity.setArea(area);
        return toDto(reservationRepository.save(entity));
    }

    @Transactional
    public ReservationDto seat(Long storeId, Long reservationId, Long requestedTableId) {
        storeAccessEnforcer.enforce(storeId);
        ReservationEntity entity = reservationRepository.findByIdAndStoreId(reservationId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!"CONFIRMED".equals(entity.getReservationStatus()) && !"PENDING".equals(entity.getReservationStatus())) {
            throw new IllegalStateException("Cannot seat reservation in status: " + entity.getReservationStatus());
        }

        StoreTableEntity targetTable;
        if (requestedTableId != null) {
            targetTable = storeTableRepository.findByIdAndStoreId(requestedTableId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + requestedTableId));
        } else if (entity.getTableId() != null) {
            // Use pre-assigned table from reservation
            targetTable = storeTableRepository.findByIdAndStoreId(entity.getTableId(), storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Pre-assigned table not found: " + entity.getTableId()));
        } else {
            targetTable = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                    .filter(table -> "AVAILABLE".equalsIgnoreCase(table.getTableStatus()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available table found for seating."));
        }

        if (!"AVAILABLE".equalsIgnoreCase(targetTable.getTableStatus())
                && !"RESERVED".equalsIgnoreCase(targetTable.getTableStatus())) {
            throw new IllegalStateException("Target table is not available for seating. Status: " + targetTable.getTableStatus());
        }

        // Create TableSession — use "OPEN" to match the rest of the POS chain
        TableSessionEntity session = new TableSessionEntity();
        session.setSessionId("SES" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        session.setMerchantId(entity.getMerchantId());
        session.setStoreId(storeId);
        session.setTableId(targetTable.getId());
        session.setSessionStatus("OPEN");
        session.setGuestCount(entity.getPartySize());
        tableSessionRepository.save(session);

        // Mark table as OCCUPIED — guest is physically seated
        targetTable.setTableStatus("OCCUPIED");
        storeTableRepository.save(targetTable);

        entity.setTableId(targetTable.getId());
        entity.setReservationStatus("CHECKED_IN");
        return toDto(reservationRepository.save(entity));
    }

    private ReservationDto toDto(ReservationEntity entity) {
        return new ReservationDto(
                entity.getId(), entity.getReservationNo(), entity.getStoreId(),
                entity.getTableId(), entity.getGuestName(), entity.getContactPhone(),
                entity.getReservationTime(), entity.getPartySize(),
                entity.getReservationStatus(), entity.getArea()
        );
    }
}
