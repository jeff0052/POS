package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.kitchen.application.dto.KitchenStationDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenStationRepository;
import com.developer.pos.v2.kitchen.interfaces.rest.request.CreateStationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KitchenStationService implements UseCase {

    private final JpaKitchenStationRepository stationRepository;
    private final StoreAccessEnforcer enforcer;

    public KitchenStationService(JpaKitchenStationRepository stationRepository,
                                  StoreAccessEnforcer enforcer) {
        this.stationRepository = stationRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public KitchenStationDto createStation(Long storeId, CreateStationRequest req) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("KDS_MANAGE");
        if (stationRepository.existsByStoreIdAndStationCode(storeId, req.stationCode())) {
            throw new IllegalArgumentException(
                "Station code already exists in this store: " + req.stationCode());
        }
        KitchenStationEntity entity = new KitchenStationEntity(
            storeId, req.stationCode(), req.stationName(), req.sortOrder());
        if (req.printerIp() != null) entity.setPrinterIp(req.printerIp());
        if (req.fallbackPrinterIp() != null) entity.setFallbackPrinterIp(req.fallbackPrinterIp());
        if (req.fallbackPrinterIp() != null) entity.setFallbackMode("AUTO");
        return toDto(stationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<KitchenStationDto> listStations(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("KDS_OPERATE");
        return stationRepository
            .findByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(storeId, "ACTIVE")
            .stream().map(this::toDto).toList();
    }

    KitchenStationDto toDto(KitchenStationEntity e) {
        return new KitchenStationDto(e.getId(), e.getStoreId(), e.getStationCode(),
            e.getStationName(), e.getStationType(), e.getPrinterIp(), e.getFallbackPrinterIp(),
            e.getFallbackMode(), e.getKdsHealthStatus(), e.getLastHeartbeatAt(),
            e.getStationStatus(), e.getSortOrder());
    }
}
