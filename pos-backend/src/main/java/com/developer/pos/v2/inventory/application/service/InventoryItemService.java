package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.inventory.application.dto.InventoryItemDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryItemService implements UseCase {

    private final JpaInventoryItemRepository itemRepository;
    private final StoreAccessEnforcer enforcer;

    public InventoryItemService(JpaInventoryItemRepository itemRepository,
                                 StoreAccessEnforcer enforcer) {
        this.itemRepository = itemRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public InventoryItemDto createItem(Long storeId, String itemCode, String itemName,
                                        String unit, BigDecimal safetyStock) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_MANAGE");
        if (itemRepository.existsByStoreIdAndItemCode(storeId, itemCode)) {
            throw new IllegalArgumentException("Item code already exists in store: " + itemCode);
        }
        InventoryItemEntity item = new InventoryItemEntity(storeId, itemCode, itemName, unit, safetyStock);
        return toDto(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listItems(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");
        return itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(storeId, "ACTIVE")
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemDto getItem(Long storeId, Long itemId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");
        InventoryItemEntity item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));
        if (!item.getStoreId().equals(storeId)) {
            throw new SecurityException("Item " + itemId + " does not belong to store " + storeId);
        }
        return toDto(item);
    }

    @Transactional
    public InventoryItemDto updateItem(Long storeId, Long itemId,
                                       String itemName, String category,
                                       BigDecimal safetyStock, Long defaultSupplierId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_MANAGE");
        InventoryItemEntity item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));
        if (!item.getStoreId().equals(storeId)) {
            throw new SecurityException("Item " + itemId + " does not belong to store " + storeId);
        }
        if (itemName != null) item.setItemName(itemName);
        if (category != null) item.setCategory(category);
        if (safetyStock != null) item.setSafetyStock(safetyStock);
        if (defaultSupplierId != null) item.setDefaultSupplierId(defaultSupplierId);
        return toDto(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemDto deactivateItem(Long storeId, Long itemId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_MANAGE");
        InventoryItemEntity item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));
        if (!item.getStoreId().equals(storeId)) {
            throw new SecurityException("Item " + itemId + " does not belong to store " + storeId);
        }
        item.deactivate();
        return toDto(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listLowStockItems(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");
        return itemRepository.findLowStockByStoreId(storeId)
                .stream().map(this::toDto).toList();
    }

    private InventoryItemDto toDto(InventoryItemEntity e) {
        return new InventoryItemDto(e.getId(), e.getStoreId(), e.getItemCode(), e.getItemName(),
            e.getCategory(), e.getUnit(), e.getCurrentStock(), e.getSafetyStock(),
            e.getItemStatus(), e.getDefaultSupplierId());
    }
}
