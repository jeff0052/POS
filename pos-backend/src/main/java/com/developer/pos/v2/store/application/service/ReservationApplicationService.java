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

        // Table assignment only allowed for CONFIRMED reservations
        if (tableId != null && !"CONFIRMED".equals(entity.getReservationStatus())) {
            throw new IllegalStateException("Cannot reserve a table for non-CONFIRMED reservation. Status: " + entity.getReservationStatus());
        }

        Long resolvedTableId = tableId;
        if (resolvedTableId == null && "CONFIRMED".equals(entity.getReservationStatus())) {
            resolvedTableId = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                    .filter(t -> "AVAILABLE".equalsIgnoreCase(t.getTableStatus()))
                    .map(StoreTableEntity::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot confirm reservation: no available table in store " + storeId));
        }
        final Long assignedTableId = resolvedTableId;

        if (assignedTableId != null) {
            StoreTableEntity table = storeTableRepository.findByIdAndStoreId(assignedTableId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + assignedTableId));
            if (!"AVAILABLE".equalsIgnoreCase(table.getTableStatus())) {
                throw new IllegalStateException("Table is not available for reservation.");
            }
            table.setTableStatus("RESERVED");
            storeTableRepository.save(table);
            entity.setTableId(assignedTableId);
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
        String currentStatus = entity.getReservationStatus();

        // Release pre-assigned table when leaving CONFIRMED/CHECKED_IN
        if (entity.getTableId() != null && !nextStatus.equals(currentStatus)) {
            boolean leavingConfirmed = "CONFIRMED".equals(currentStatus)
                    && !"CHECKED_IN".equals(nextStatus);
            boolean leavingCheckedIn = "CHECKED_IN".equals(currentStatus)
                    && !"CHECKED_IN".equals(nextStatus);

            if (leavingConfirmed || leavingCheckedIn) {
                storeTableRepository.findByIdAndStoreId(entity.getTableId(), storeId).ifPresent(table -> {
                    String ts = table.getTableStatus();
                    if ("RESERVED".equalsIgnoreCase(ts) || "OCCUPIED".equalsIgnoreCase(ts)) {
                        table.setTableStatus("AVAILABLE");
                        storeTableRepository.save(table);
                    }
                });
                entity.setTableId(null);
            }
        }

        entity.setGuestName(guestName);
        entity.setContactPhone(contactPhone);
        entity.setReservationTime(reservationTime);
        entity.setPartySize(partySize);
        entity.setReservationStatus(nextStatus);
        entity.setArea(area);

        // When transitioning INTO CONFIRMED and no table assigned, auto-assign one
        if ("CONFIRMED".equals(nextStatus) && entity.getTableId() == null) {
            Long autoTableId = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                    .filter(t -> "AVAILABLE".equalsIgnoreCase(t.getTableStatus()))
                    .map(StoreTableEntity::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot confirm reservation: no available table in store " + storeId));
            StoreTableEntity table = storeTableRepository.findByIdAndStoreId(autoTableId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + autoTableId));
            table.setTableStatus("RESERVED");
            storeTableRepository.save(table);
            entity.setTableId(autoTableId);
        }

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

        // Determine target table
        StoreTableEntity targetTable;
        if (requestedTableId != null) {
            targetTable = storeTableRepository.findByIdAndStoreId(requestedTableId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + requestedTableId));
        } else if (entity.getTableId() != null) {
            // Use pre-assigned table
            targetTable = storeTableRepository.findByIdAndStoreId(entity.getTableId(), storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Pre-assigned table not found: " + entity.getTableId()));
        } else {
            targetTable = storeTableRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                    .filter(t -> "AVAILABLE".equalsIgnoreCase(t.getTableStatus()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available table found for seating."));
        }

        if (!"AVAILABLE".equalsIgnoreCase(targetTable.getTableStatus())
                && !"RESERVED".equalsIgnoreCase(targetTable.getTableStatus())) {
            throw new IllegalStateException("Target table is not available for seating. Status: " + targetTable.getTableStatus());
        }

        // If seating at a different table than pre-assigned, release the old one
        if (entity.getTableId() != null && !entity.getTableId().equals(targetTable.getId())) {
            storeTableRepository.findByIdAndStoreId(entity.getTableId(), storeId).ifPresent(oldTable -> {
                if ("RESERVED".equalsIgnoreCase(oldTable.getTableStatus())) {
                    oldTable.setTableStatus("AVAILABLE");
                    storeTableRepository.save(oldTable);
                }
            });
        }

        // Create TableSession — "OPEN" matches the rest of the POS chain
        TableSessionEntity session = new TableSessionEntity();
        session.setSessionId("SES" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        session.setMerchantId(entity.getMerchantId());
        session.setStoreId(storeId);
        session.setTableId(targetTable.getId());
        session.setSessionStatus("OPEN");
        session.setGuestCount(entity.getPartySize());
        tableSessionRepository.save(session);

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
