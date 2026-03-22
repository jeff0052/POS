package com.developer.pos.store.service;

import com.developer.pos.store.dto.StoreDto;
import com.developer.pos.store.dto.StoreSettingsDto;
import com.developer.pos.store.entity.StoreEntity;
import com.developer.pos.store.entity.StoreSettingsEntity;
import com.developer.pos.store.repository.StoreRepository;
import com.developer.pos.store.repository.StoreSettingsRepository;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public StoreService(StoreRepository storeRepository, StoreSettingsRepository storeSettingsRepository) {
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    public StoreDto getCurrentStore() {
        return storeRepository.findById(1001L)
            .map(this::toStoreDto)
            .orElse(new StoreDto(1001L, "Demo Store", "STORE1001", "Shanghai", "13800000000"));
    }

    public StoreSettingsDto getCurrentSettings() {
        return storeSettingsRepository.findByStoreId(1001L)
            .map(this::toSettingsDto)
            .orElse(new StoreSettingsDto(1001L, "Demo Store", "Thanks for visiting", Collections.emptyMap(), Collections.emptyMap()));
    }

    public StoreSettingsDto updateSettings(StoreSettingsDto settingsDto) {
        return settingsDto;
    }

    private StoreDto toStoreDto(StoreEntity entity) {
        return new StoreDto(
            entity.getId(),
            entity.getStoreName(),
            entity.getStoreCode(),
            entity.getAddress(),
            entity.getPhone()
        );
    }

    private StoreSettingsDto toSettingsDto(StoreSettingsEntity entity) {
        return new StoreSettingsDto(
            entity.getStoreId(),
            entity.getReceiptTitle(),
            entity.getReceiptFooter(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }
}
