package com.developer.pos.v2.platform.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PlatformDashboardService implements UseCase {

    private final JpaStoreRepository storeRepository;

    public PlatformDashboardService(JpaStoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        long totalStores = storeRepository.count();
        return Map.of(
                "totalMerchants", 1,
                "totalStores", totalStores,
                "activeStores", totalStores,
                "totalDevices", 0,
                "systemStatus", "HEALTHY"
        );
    }
}
