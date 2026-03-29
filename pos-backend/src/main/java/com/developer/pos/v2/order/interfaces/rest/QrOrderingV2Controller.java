package com.developer.pos.v2.order.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.catalog.application.dto.QrMenuDto;
import com.developer.pos.v2.catalog.application.service.QrMenuApplicationService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.order.application.command.SubmitQrOrderingCommand;
import com.developer.pos.v2.order.application.dto.QrOrderingContextDto;
import com.developer.pos.v2.order.application.dto.QrOrderingSubmitResultDto;
import com.developer.pos.v2.order.application.service.ActiveTableOrderApplicationService;
import com.developer.pos.v2.order.interfaces.rest.request.QrOrderingSubmitRequest;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/qr-ordering")
public class QrOrderingV2Controller implements V2Api {

    private final QrMenuApplicationService qrMenuApplicationService;
    private final ActiveTableOrderApplicationService activeTableOrderApplicationService;
    private final JpaStoreLookupRepository storeLookupRepository;

    public QrOrderingV2Controller(
            QrMenuApplicationService qrMenuApplicationService,
            ActiveTableOrderApplicationService activeTableOrderApplicationService,
            JpaStoreLookupRepository storeLookupRepository
    ) {
        this.qrMenuApplicationService = qrMenuApplicationService;
        this.activeTableOrderApplicationService = activeTableOrderApplicationService;
        this.storeLookupRepository = storeLookupRepository;
    }

    @GetMapping("/menu")
    public ApiResponse<QrMenuDto> getMenu(@RequestParam String storeCode) {
        return ApiResponse.success(qrMenuApplicationService.getMenu(storeCode));
    }

    @GetMapping("/context")
    public ApiResponse<QrOrderingContextDto> getContext(
            @RequestParam String storeCode,
            @RequestParam String tableCode,
            HttpServletRequest httpRequest
    ) {
        enforceTokenBinding(httpRequest, storeCode, tableCode);
        return ApiResponse.success(activeTableOrderApplicationService.getQrOrderingContext(storeCode, tableCode));
    }

    @PostMapping("/submit")
    public ApiResponse<QrOrderingSubmitResultDto> submit(
            @Valid @RequestBody QrOrderingSubmitRequest request,
            HttpServletRequest httpRequest
    ) {
        enforceTokenBinding(httpRequest, request.storeCode(), request.tableCode());

        SubmitQrOrderingCommand command = new SubmitQrOrderingCommand(
                request.storeCode(),
                request.tableCode(),
                request.memberId(),
                request.items().stream()
                        .map(item -> new SubmitQrOrderingCommand.SubmitQrOrderingItemInput(
                                item.skuId(),
                                item.skuCode(),
                                item.skuName(),
                                item.quantity(),
                                item.unitPriceCents(),
                                item.remark()
                        ))
                        .toList()
        );

        return ApiResponse.success(activeTableOrderApplicationService.submitQrOrdering(command));
    }

    /**
     * Verify that the storeCode and tableCode in the request match the claims in the ordering JWT.
     * Prevents cross-store and cross-table ordering.
     */
    private void enforceTokenBinding(HttpServletRequest request, String requestStoreCode, String requestTableCode) {
        Long tokenStoreId = (Long) request.getAttribute("qr.storeId");
        String tokenTableCode = (String) request.getAttribute("qr.tableCode");

        if (tokenStoreId == null || tokenTableCode == null) {
            throw new IllegalStateException("Ordering token claims not found. Is QrOrderingFilter active?");
        }

        // Verify store identity: resolve storeCode and compare with token's storeId
        StoreEntity store = storeLookupRepository.findByStoreCode(requestStoreCode)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + requestStoreCode));
        if (!tokenStoreId.equals(store.getId())) {
            throw new IllegalArgumentException(
                    "Store mismatch: token is bound to store " + tokenStoreId +
                    " but request targets store " + store.getId());
        }

        // Verify table identity
        if (!tokenTableCode.equals(requestTableCode)) {
            throw new IllegalArgumentException(
                    "Table code mismatch: token is bound to table " + tokenTableCode +
                    " but request targets table " + requestTableCode);
        }
    }
}
