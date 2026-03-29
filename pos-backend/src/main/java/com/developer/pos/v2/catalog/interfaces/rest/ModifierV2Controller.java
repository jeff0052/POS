package com.developer.pos.v2.catalog.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.ModifierGroupDetailDto;
import com.developer.pos.v2.catalog.application.service.ModifierManagementService;
import com.developer.pos.v2.catalog.interfaces.rest.request.BindSkuModifierRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpsertModifierGroupRequest;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/modifiers")
public class ModifierV2Controller implements V2Api {

    private final ModifierManagementService modifierManagementService;

    public ModifierV2Controller(ModifierManagementService modifierManagementService) {
        this.modifierManagementService = modifierManagementService;
    }

    @GetMapping("/groups")
    public ApiResponse<List<ModifierGroupDetailDto>> listGroups() {
        return ApiResponse.success(modifierManagementService.listGroups());
    }

    @GetMapping("/groups/{groupId}")
    public ApiResponse<ModifierGroupDetailDto> getGroup(@PathVariable Long groupId) {
        return ApiResponse.success(modifierManagementService.getGroup(groupId));
    }

    @PostMapping("/groups")
    public ApiResponse<ModifierGroupDetailDto> createGroup(@Valid @RequestBody UpsertModifierGroupRequest request) {
        return ApiResponse.success(modifierManagementService.createGroup(
                request.groupCode(), request.groupName(), request.selectionType(),
                request.required(), request.minSelect(), request.maxSelect(), request.sortOrder(),
                toOptionCommands(request.options())));
    }

    @PutMapping("/groups/{groupId}")
    public ApiResponse<ModifierGroupDetailDto> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpsertModifierGroupRequest request
    ) {
        return ApiResponse.success(modifierManagementService.updateGroup(
                groupId, request.groupCode(), request.groupName(), request.selectionType(),
                request.required(), request.minSelect(), request.maxSelect(), request.sortOrder(),
                toOptionCommands(request.options())));
    }

    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long groupId) {
        modifierManagementService.deleteGroup(groupId);
        return ApiResponse.success(null);
    }

    @PostMapping("/sku-bindings")
    public ApiResponse<List<ModifierGroupDetailDto>> bindSkuModifiers(@Valid @RequestBody BindSkuModifierRequest request) {
        List<ModifierManagementService.BindingCommand> bindings = request.bindings().stream()
                .map(b -> new ModifierManagementService.BindingCommand(b.modifierGroupId(), b.sortOrder()))
                .toList();
        return ApiResponse.success(modifierManagementService.bindSkuModifiers(request.skuId(), bindings));
    }

    @GetMapping("/sku-bindings/{skuId}")
    public ApiResponse<List<ModifierGroupDetailDto>> getSkuModifiers(@PathVariable Long skuId) {
        return ApiResponse.success(modifierManagementService.getSkuModifiers(skuId));
    }

    private List<ModifierManagementService.CreateOptionCommand> toOptionCommands(
            List<UpsertModifierGroupRequest.OptionItem> options
    ) {
        if (options == null) return List.of();
        return options.stream()
                .map(o -> new ModifierManagementService.CreateOptionCommand(
                        o.optionCode(), o.optionName(), o.priceAdjustmentCents(),
                        o.defaultOption(), o.sortOrder()))
                .toList();
    }
}
