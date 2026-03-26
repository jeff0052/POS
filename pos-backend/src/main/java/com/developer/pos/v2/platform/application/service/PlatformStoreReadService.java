package com.developer.pos.v2.platform.application.service;

import com.developer.pos.v2.platform.application.dto.PlatformStoreOverviewDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformStoreReadService {

    private final JpaStoreRepository storeRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public PlatformStoreReadService(
            JpaStoreRepository storeRepository,
            JpaStoreTableRepository storeTableRepository
    ) {
        this.storeRepository = storeRepository;
        this.storeTableRepository = storeTableRepository;
    }

    public List<PlatformStoreOverviewDto> listStoreOverview() {
        List<StoreEntity> stores = storeRepository.findAll();
        Map<Long, List<StoreTableEntity>> tablesByStore = storeTableRepository.findAll().stream()
                .collect(Collectors.groupingBy(StoreTableEntity::getStoreId));

        return stores.stream()
                .map(store -> {
                    List<StoreTableEntity> tables = tablesByStore.getOrDefault(store.getId(), List.of());
                    long availableTables = tables.stream().filter(table -> "AVAILABLE".equalsIgnoreCase(table.getTableStatus())).count();
                    long reservedTables = tables.stream().filter(table -> "RESERVED".equalsIgnoreCase(table.getTableStatus())).count();
                    long pendingSettlementTables = tables.stream().filter(table -> "PENDING_SETTLEMENT".equalsIgnoreCase(table.getTableStatus())).count();
                    long occupiedTables = tables.size() - availableTables;

                    return new PlatformStoreOverviewDto(
                            store.getId(),
                            store.getMerchantId(),
                            store.getStoreCode(),
                            store.getStoreName(),
                            tables.size(),
                            availableTables,
                            occupiedTables,
                            reservedTables,
                            pendingSettlementTables
                    );
                })
                .sorted((left, right) -> Long.compare(left.storeId(), right.storeId()))
                .toList();
    }
}
