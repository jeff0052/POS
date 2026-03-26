package com.developer.pos.v2.platform.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.platform.application.dto.MerchantSummaryDto;
import com.developer.pos.v2.platform.application.dto.StoreSummaryDto;
import com.developer.pos.v2.platform.application.dto.PlatformDashboardDto;
import com.developer.pos.v2.platform.application.command.CreateMerchantCommand;
import com.developer.pos.v2.platform.application.command.CreateStoreCommand;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlatformAdminApplicationService implements UseCase {

    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaStoreRepository storeRepository;

    public PlatformAdminApplicationService(JpaStoreLookupRepository storeLookupRepository,
                                           JpaStoreRepository storeRepository) {
        this.storeLookupRepository = storeLookupRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public PlatformDashboardDto getDashboard() {
        long totalStores = storeRepository.count();
        return new PlatformDashboardDto(totalStores);
    }

    @Transactional(readOnly = true)
    public List<StoreSummaryDto> listStores() {
        return storeRepository.findAll().stream()
                .map(store -> new StoreSummaryDto(
                        store.getId(), store.getMerchantId(), store.getStoreCode(),
                        store.getStoreName(), store.getStoreStatus()
                )).toList();
    }

    @Transactional(readOnly = true)
    public StoreSummaryDto getStore(Long storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        return new StoreSummaryDto(store.getId(), store.getMerchantId(), store.getStoreCode(),
                store.getStoreName(), store.getStoreStatus());
    }

    @Transactional
    public StoreSummaryDto createStore(CreateStoreCommand command) {
        StoreEntity store = new StoreEntity();
        store.setMerchantId(command.merchantId());
        store.setStoreCode("S" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        store.setStoreName(command.storeName());
        store.setStoreStatus("ACTIVE");
        StoreEntity saved = storeRepository.save(store);
        return new StoreSummaryDto(saved.getId(), saved.getMerchantId(), saved.getStoreCode(),
                saved.getStoreName(), saved.getStoreStatus());
    }

    @Transactional
    public StoreSummaryDto updateStoreStatus(Long storeId, String status) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        store.setStoreStatus(status);
        StoreEntity saved = storeRepository.save(store);
        return new StoreSummaryDto(saved.getId(), saved.getMerchantId(), saved.getStoreCode(),
                saved.getStoreName(), saved.getStoreStatus());
    }
}
