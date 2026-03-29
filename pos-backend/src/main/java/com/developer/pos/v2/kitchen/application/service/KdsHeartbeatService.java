package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.kitchen.application.dto.StationHeartbeatDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class KdsHeartbeatService implements UseCase {

    private final JpaKitchenStationRepository stationRepository;
    private final StoreAccessEnforcer enforcer;

    public KdsHeartbeatService(JpaKitchenStationRepository stationRepository,
                                StoreAccessEnforcer enforcer) {
        this.stationRepository = stationRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public StationHeartbeatDto heartbeat(Long stationId) {
        KitchenStationEntity station = stationRepository.findById(stationId)
            .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));

        // Derive storeId from station row — caller does not provide it
        enforcer.enforce(station.getStoreId());
        enforcer.enforcePermission("KDS_OPERATE");

        station.setLastHeartbeatAt(LocalDateTime.now());
        if (!station.isOnline()) {
            station.markOnline();
        }
        stationRepository.save(station);

        return new StationHeartbeatDto(station.getId(), station.getKdsHealthStatus(),
            station.getLastHeartbeatAt());
    }
}
