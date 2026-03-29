package com.developer.pos.v2.store.application.service;

import com.developer.pos.v2.store.infrastructure.persistence.entity.ReservationEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaReservationRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpirySchedulerTest {

    @Mock JpaReservationRepository reservationRepo;
    @Mock JpaStoreTableRepository tableRepo;

    @InjectMocks ReservationExpiryScheduler scheduler;

    @Test
    void cancelsExpiredReservation() {
        ReservationEntity expired = new ReservationEntity();
        expired.setReservationNo("RSV001");
        expired.setReservationStatus("CONFIRMED");
        expired.setTableId(null);

        when(reservationRepo.findExpiredReservations(anyString())).thenReturn(List.of(expired));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.cancelExpiredReservations();

        assertEquals("CANCELLED", expired.getReservationStatus());
        verify(reservationRepo).save(expired);
    }

    @Test
    void releasesReservedTableOnCancel() {
        StoreTableEntity table = new StoreTableEntity();
        table.setTableStatus("RESERVED");

        ReservationEntity expired = new ReservationEntity();
        expired.setReservationNo("RSV002");
        expired.setReservationStatus("CONFIRMED");
        expired.setTableId(5L);

        when(reservationRepo.findExpiredReservations(anyString())).thenReturn(List.of(expired));
        when(tableRepo.findById(5L)).thenReturn(Optional.of(table));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.cancelExpiredReservations();

        assertEquals("CANCELLED", expired.getReservationStatus());
        assertNull(expired.getTableId());
        assertEquals("AVAILABLE", table.getTableStatus());
    }

    @Test
    void doesNotTouchNonExpiredReservations() {
        when(reservationRepo.findExpiredReservations(anyString())).thenReturn(List.of());

        scheduler.cancelExpiredReservations();

        verify(reservationRepo, never()).save(any());
    }
}
