package com.developer.pos.v2.store.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.application.dto.ReservationDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.ReservationEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaReservationRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationApplicationServiceTest {

    @Mock JpaReservationRepository reservationRepo;
    @Mock JpaStoreRepository storeRepo;
    @Mock JpaStoreTableRepository tableRepo;
    @Mock JpaTableSessionRepository tableSessionRepo;
    @Mock StoreAccessEnforcer storeAccessEnforcer;

    @InjectMocks ReservationApplicationService service;

    @Test
    void seat_createsTableSession_withOpenStatus_andSetsTableOccupied() {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationNo("RSV001");
        reservation.setStoreId(1L);
        reservation.setMerchantId(1L);
        reservation.setPartySize(4);
        reservation.setReservationStatus("CONFIRMED");

        StoreTableEntity table = new StoreTableEntity();
        table.setTableStatus("AVAILABLE");

        when(reservationRepo.findByIdAndStoreId(10L, 1L)).thenReturn(Optional.of(reservation));
        when(tableRepo.findByIdAndStoreId(5L, 1L)).thenReturn(Optional.of(table));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tableSessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationDto result = service.seat(1L, 10L, 5L);

        assertEquals("CHECKED_IN", result.reservationStatus());
        assertEquals("OCCUPIED", table.getTableStatus());

        ArgumentCaptor<TableSessionEntity> sessionCaptor = ArgumentCaptor.forClass(TableSessionEntity.class);
        verify(tableSessionRepo).save(sessionCaptor.capture());
        TableSessionEntity session = sessionCaptor.getValue();
        assertEquals("OPEN", session.getSessionStatus()); // Must be OPEN, not ACTIVE
        assertEquals(1L, session.getStoreId());
        assertEquals(4, session.getGuestCount());
        verify(storeAccessEnforcer).enforce(1L);
    }

    @Test
    void seat_autoSelectTable_picksAvailable() {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationNo("RSV002");
        reservation.setStoreId(1L);
        reservation.setMerchantId(1L);
        reservation.setPartySize(2);
        reservation.setReservationStatus("CONFIRMED");

        StoreTableEntity occupied = new StoreTableEntity();
        occupied.setTableStatus("OCCUPIED");

        StoreTableEntity available = new StoreTableEntity();
        available.setTableStatus("AVAILABLE");

        when(reservationRepo.findByIdAndStoreId(10L, 1L)).thenReturn(Optional.of(reservation));
        when(tableRepo.findAllByStoreIdOrderByIdAsc(1L)).thenReturn(List.of(occupied, available));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tableSessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationDto result = service.seat(1L, 10L, null);

        assertEquals("CHECKED_IN", result.reservationStatus());
        assertEquals("OCCUPIED", available.getTableStatus());
        verify(tableSessionRepo).save(any());
    }

    @Test
    void seat_occupiedTable_throws() {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationNo("RSV003");
        reservation.setStoreId(1L);
        reservation.setReservationStatus("CONFIRMED");

        StoreTableEntity table = new StoreTableEntity();
        table.setTableStatus("OCCUPIED");

        when(reservationRepo.findByIdAndStoreId(10L, 1L)).thenReturn(Optional.of(reservation));
        when(tableRepo.findByIdAndStoreId(5L, 1L)).thenReturn(Optional.of(table));

        assertThrows(IllegalStateException.class, () -> service.seat(1L, 10L, 5L));
    }

    @Test
    void seat_preAssignedReservedTable_succeeds() {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationNo("RSV004");
        reservation.setStoreId(1L);
        reservation.setMerchantId(1L);
        reservation.setPartySize(3);
        reservation.setReservationStatus("CONFIRMED");
        reservation.setTableId(7L); // pre-assigned during create

        StoreTableEntity preAssigned = new StoreTableEntity();
        preAssigned.setTableStatus("RESERVED"); // table was RESERVED at create time

        when(reservationRepo.findByIdAndStoreId(10L, 1L)).thenReturn(Optional.of(reservation));
        when(tableRepo.findByIdAndStoreId(7L, 1L)).thenReturn(Optional.of(preAssigned));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tableSessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationDto result = service.seat(1L, 10L, null); // no explicit tableId, uses pre-assigned

        assertEquals("CHECKED_IN", result.reservationStatus());
        assertEquals("OCCUPIED", preAssigned.getTableStatus());
        verify(tableSessionRepo).save(any());
    }
}
