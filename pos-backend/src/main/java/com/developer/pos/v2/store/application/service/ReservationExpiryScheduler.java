package com.developer.pos.v2.store.application.service;

import com.developer.pos.v2.store.infrastructure.persistence.entity.ReservationEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaReservationRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JpaReservationRepository reservationRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public ReservationExpiryScheduler(
            JpaReservationRepository reservationRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.storeTableRepository = storeTableRepository;
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    @Transactional
    public void cancelExpiredReservations() {
        String cutoff = LocalDateTime.now().minusMinutes(30).format(FMT);
        List<ReservationEntity> expired = reservationRepository.findExpiredReservations(cutoff);

        for (ReservationEntity reservation : expired) {
            reservation.setReservationStatus("NO_SHOW");

            if (reservation.getTableId() != null) {
                storeTableRepository.findById(reservation.getTableId()).ifPresent(table -> {
                    if ("RESERVED".equalsIgnoreCase(table.getTableStatus())) {
                        table.setTableStatus("AVAILABLE");
                        storeTableRepository.save(table);
                    }
                });
                reservation.setTableId(null);
            }

            reservationRepository.save(reservation);
            log.info("Marked NO_SHOW for expired reservation: {}", reservation.getReservationNo());
        }

        if (!expired.isEmpty()) {
            log.info("Marked {} expired reservations as NO_SHOW", expired.size());
        }
    }
}
