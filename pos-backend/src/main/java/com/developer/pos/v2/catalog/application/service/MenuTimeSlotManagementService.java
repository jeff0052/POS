package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.MenuTimeSlotDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaMenuTimeSlotProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaMenuTimeSlotRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class MenuTimeSlotManagementService implements UseCase {

    private final JpaMenuTimeSlotRepository slotRepository;
    private final JpaMenuTimeSlotProductRepository slotProductRepository;
    private final JpaStoreLookupRepository storeLookupRepository;
    private final ObjectMapper objectMapper;

    public MenuTimeSlotManagementService(
            JpaMenuTimeSlotRepository slotRepository,
            JpaMenuTimeSlotProductRepository slotProductRepository,
            JpaStoreLookupRepository storeLookupRepository,
            ObjectMapper objectMapper
    ) {
        this.slotRepository = slotRepository;
        this.slotProductRepository = slotProductRepository;
        this.storeLookupRepository = storeLookupRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<MenuTimeSlotDto> listSlots(Long storeId) {
        enforceStoreAccess(storeId);
        List<MenuTimeSlotEntity> slots = slotRepository.findByStoreIdOrderByPriorityDescStartTimeAsc(storeId);
        return slots.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MenuTimeSlotDto getSlot(Long slotId, Long expectedStoreId) {
        enforceStoreAccess(expectedStoreId);
        MenuTimeSlotEntity slot = findSlotAndEnforceAccess(slotId);
        enforceSlotStoreMatch(slot, expectedStoreId);
        return toDto(slot);
    }

    @Transactional
    public MenuTimeSlotDto createSlot(Long storeId, String slotCode, String slotName,
                                      LocalTime startTime, LocalTime endTime,
                                      List<String> applicableDays, List<String> diningModes,
                                      boolean active, int priority, List<Long> productIds) {
        enforceMenuManage();
        enforceStoreAccess(storeId);
        validateTimeRange(startTime, endTime);
        slotRepository.findByStoreIdAndSlotCode(storeId, slotCode).ifPresent(existing -> {
            throw new IllegalArgumentException("Time slot code already exists: " + slotCode);
        });

        MenuTimeSlotEntity slot = new MenuTimeSlotEntity(
                storeId, slotCode, slotName, startTime, endTime,
                writeJson(applicableDays), writeJson(diningModes), active, priority);
        slot = slotRepository.save(slot);

        saveProductBindings(slot.getId(), productIds);
        return toDto(slot);
    }

    @Transactional
    public MenuTimeSlotDto updateSlot(Long slotId, String slotCode, String slotName,
                                      LocalTime startTime, LocalTime endTime,
                                      List<String> applicableDays, List<String> diningModes,
                                      boolean active, int priority, List<Long> productIds) {
        enforceMenuManage();
        MenuTimeSlotEntity slot = findSlotAndEnforceAccess(slotId);
        validateTimeRange(startTime, endTime);
        slot.update(slotCode, slotName, startTime, endTime,
                writeJson(applicableDays), writeJson(diningModes), active, priority);
        slot = slotRepository.save(slot);

        slotProductRepository.deleteByTimeSlotId(slot.getId());
        saveProductBindings(slot.getId(), productIds);
        return toDto(slot);
    }

    @Transactional
    public void deleteSlot(Long slotId, Long expectedStoreId) {
        enforceMenuManage();
        enforceStoreAccess(expectedStoreId);
        MenuTimeSlotEntity slot = findSlotAndEnforceAccess(slotId);
        enforceSlotStoreMatch(slot, expectedStoreId);
        slotProductRepository.deleteByTimeSlotId(slot.getId());
        slotRepository.delete(slot);
    }

    private void saveProductBindings(Long slotId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        for (Long productId : productIds) {
            slotProductRepository.save(new MenuTimeSlotProductEntity(slotId, productId, true));
        }
    }

    private MenuTimeSlotEntity findSlotAndEnforceAccess(Long slotId) {
        MenuTimeSlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found: " + slotId));
        enforceStoreAccess(slot.getStoreId());
        return slot;
    }

    private void enforceMenuManage() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("MENU_MANAGE")) {
            throw new SecurityException("MENU_MANAGE permission required");
        }
    }

    private void enforceSlotStoreMatch(MenuTimeSlotEntity slot, Long expectedStoreId) {
        if (expectedStoreId != null && !Objects.equals(slot.getStoreId(), expectedStoreId)) {
            throw new IllegalArgumentException("Time slot does not belong to store: " + expectedStoreId);
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime: " + startTime + " >= " + endTime);
        }
    }

    private void enforceStoreAccess(Long storeId) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() != null && actor.merchantId() != 0L) {
            StoreEntity store = storeLookupRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
            if (!Objects.equals(store.getMerchantId(), actor.merchantId())) {
                throw new SecurityException("Store does not belong to your merchant");
            }
        }
    }

    private MenuTimeSlotDto toDto(MenuTimeSlotEntity slot) {
        List<MenuTimeSlotProductEntity> products = slotProductRepository.findByTimeSlotIdAndIsVisible(slot.getId(), true);
        List<Long> productIds = products.stream().map(MenuTimeSlotProductEntity::getProductId).toList();
        return new MenuTimeSlotDto(
                slot.getId(), slot.getSlotCode(), slot.getSlotName(),
                slot.getStartTime(), slot.getEndTime(),
                readJson(slot.getApplicableDays()),
                readJson(slot.getDiningModes()),
                slot.isActive(), slot.getPriority(), productIds);
    }

    private String writeJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize JSON", e);
        }
    }

    private List<String> readJson(String raw) {
        try {
            return raw == null || raw.isBlank()
                    ? List.of()
                    : objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
