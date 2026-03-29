package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.ModifierGroupDetailDto;
import com.developer.pos.v2.catalog.application.dto.ModifierOptionDetailDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierGroupEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierOptionEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuModifierGroupBindingEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaModifierGroupRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaModifierOptionRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuModifierGroupBindingRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModifierManagementService implements UseCase {

    private final JpaModifierGroupRepository groupRepository;
    private final JpaModifierOptionRepository optionRepository;
    private final JpaSkuModifierGroupBindingRepository bindingRepository;
    private final JpaSkuRepository skuRepository;

    public ModifierManagementService(
            JpaModifierGroupRepository groupRepository,
            JpaModifierOptionRepository optionRepository,
            JpaSkuModifierGroupBindingRepository bindingRepository,
            JpaSkuRepository skuRepository
    ) {
        this.groupRepository = groupRepository;
        this.optionRepository = optionRepository;
        this.bindingRepository = bindingRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public List<ModifierGroupDetailDto> listGroups() {
        Long merchantId = enforceCallerMerchant();
        List<ModifierGroupEntity> groups = groupRepository.findByMerchantIdOrderBySortOrderAscGroupNameAsc(merchantId);
        List<Long> groupIds = groups.stream().map(ModifierGroupEntity::getId).toList();
        Map<Long, List<ModifierOptionDetailDto>> optionsMap = loadOptionsByGroupIds(groupIds);

        return groups.stream()
                .map(g -> toDto(g, optionsMap.getOrDefault(g.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ModifierGroupDetailDto getGroup(Long groupId) {
        ModifierGroupEntity group = findGroupAndEnforceMerchant(groupId);
        List<ModifierOptionEntity> options = optionRepository.findByGroupIdOrderBySortOrderAsc(group.getId());
        return toDto(group, options.stream().map(this::toOptionDto).toList());
    }

    @Transactional
    public ModifierGroupDetailDto createGroup(String groupCode, String groupName, String selectionType,
                                              boolean required, int minSelect, int maxSelect, int sortOrder,
                                              List<CreateOptionCommand> options) {
        Long merchantId = enforceCallerMerchant();
        groupRepository.findByMerchantIdAndGroupCode(merchantId, groupCode).ifPresent(existing -> {
            throw new IllegalArgumentException("Modifier group code already exists: " + groupCode);
        });

        ModifierGroupEntity group = new ModifierGroupEntity(
                merchantId, groupCode, groupName, selectionType, required, minSelect, maxSelect, sortOrder);
        group = groupRepository.save(group);

        List<ModifierOptionDetailDto> optionDtos = saveOptions(group.getId(), options);
        return toDto(group, optionDtos);
    }

    @Transactional
    public ModifierGroupDetailDto updateGroup(Long groupId, String groupCode, String groupName,
                                              String selectionType, boolean required,
                                              int minSelect, int maxSelect, int sortOrder,
                                              List<CreateOptionCommand> options) {
        ModifierGroupEntity group = findGroupAndEnforceMerchant(groupId);
        group.update(groupCode, groupName, selectionType, required, minSelect, maxSelect, sortOrder);
        group = groupRepository.save(group);

        optionRepository.deleteByGroupId(group.getId());
        List<ModifierOptionDetailDto> optionDtos = saveOptions(group.getId(), options);
        return toDto(group, optionDtos);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        ModifierGroupEntity group = findGroupAndEnforceMerchant(groupId);
        optionRepository.deleteByGroupId(group.getId());
        groupRepository.delete(group);
    }

    @Transactional
    public List<ModifierGroupDetailDto> bindSkuModifiers(Long skuId, List<BindingCommand> bindings) {
        Long merchantId = enforceCallerMerchant();
        SkuEntity sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));

        bindingRepository.deleteBySkuId(skuId);

        List<Long> groupIds = new ArrayList<>();
        for (BindingCommand binding : bindings) {
            ModifierGroupEntity group = groupRepository.findById(binding.modifierGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Modifier group not found: " + binding.modifierGroupId()));
            if (!group.getMerchantId().equals(merchantId)) {
                throw new SecurityException("Modifier group does not belong to your merchant: " + binding.modifierGroupId());
            }
            bindingRepository.save(new SkuModifierGroupBindingEntity(skuId, binding.modifierGroupId(), binding.sortOrder()));
            groupIds.add(binding.modifierGroupId());
        }

        List<ModifierGroupEntity> groups = groupRepository.findByIdInOrderBySortOrderAsc(groupIds);
        Map<Long, List<ModifierOptionDetailDto>> optionsMap = loadOptionsByGroupIds(groupIds);
        return groups.stream()
                .map(g -> toDto(g, optionsMap.getOrDefault(g.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ModifierGroupDetailDto> getSkuModifiers(Long skuId) {
        List<SkuModifierGroupBindingEntity> bindings = bindingRepository.findBySkuIdOrderBySortOrderAsc(skuId);
        List<Long> groupIds = bindings.stream().map(SkuModifierGroupBindingEntity::getModifierGroupId).toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        List<ModifierGroupEntity> groups = groupRepository.findByIdInOrderBySortOrderAsc(groupIds);
        Map<Long, List<ModifierOptionDetailDto>> optionsMap = loadOptionsByGroupIds(groupIds);
        return groups.stream()
                .map(g -> toDto(g, optionsMap.getOrDefault(g.getId(), List.of())))
                .toList();
    }

    private List<ModifierOptionDetailDto> saveOptions(Long groupId, List<CreateOptionCommand> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<ModifierOptionDetailDto> result = new ArrayList<>();
        for (CreateOptionCommand cmd : options) {
            ModifierOptionEntity entity = new ModifierOptionEntity(
                    groupId, cmd.optionCode(), cmd.optionName(),
                    cmd.priceAdjustmentCents(), cmd.defaultOption(), cmd.sortOrder());
            entity = optionRepository.save(entity);
            result.add(toOptionDto(entity));
        }
        return result;
    }

    private Map<Long, List<ModifierOptionDetailDto>> loadOptionsByGroupIds(List<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        List<ModifierOptionEntity> allOptions = optionRepository.findByGroupIdInOrderByGroupIdAscSortOrderAsc(groupIds);
        Map<Long, List<ModifierOptionDetailDto>> map = new HashMap<>();
        for (ModifierOptionEntity option : allOptions) {
            map.computeIfAbsent(option.getGroupId(), k -> new ArrayList<>()).add(toOptionDto(option));
        }
        return map;
    }

    private ModifierGroupEntity findGroupAndEnforceMerchant(Long groupId) {
        ModifierGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Modifier group not found: " + groupId));
        Long merchantId = enforceCallerMerchant();
        if (!group.getMerchantId().equals(merchantId)) {
            throw new SecurityException("Modifier group does not belong to your merchant");
        }
        return group;
    }

    private Long enforceCallerMerchant() {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() == null || actor.merchantId() == 0L) {
            throw new IllegalArgumentException("Merchant context required for modifier management");
        }
        return actor.merchantId();
    }

    private ModifierGroupDetailDto toDto(ModifierGroupEntity g, List<ModifierOptionDetailDto> options) {
        return new ModifierGroupDetailDto(
                g.getId(), g.getGroupCode(), g.getGroupName(), g.getSelectionType(),
                g.isRequired(), g.getMinSelect(), g.getMaxSelect(), g.getSortOrder(), options);
    }

    private ModifierOptionDetailDto toOptionDto(ModifierOptionEntity o) {
        return new ModifierOptionDetailDto(
                o.getId(), o.getOptionCode(), o.getOptionName(),
                o.getPriceAdjustmentCents(), o.isDefaultOption(), o.getSortOrder());
    }

    public record CreateOptionCommand(
            String optionCode, String optionName,
            long priceAdjustmentCents, boolean defaultOption, int sortOrder
    ) {
    }

    public record BindingCommand(Long modifierGroupId, int sortOrder) {
    }
}
