package com.developer.pos.v2.order.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.order.application.command.SubmitQrOrderingCommand;
import com.developer.pos.v2.order.application.dto.QrOrderingSubmitResultDto;
import com.developer.pos.v2.order.application.service.ActiveTableOrderApplicationService;
import com.developer.pos.v2.order.interfaces.rest.request.QrOrderingSubmitRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/qr-ordering")
public class QrOrderingV2Controller implements V2Api {

    private final ActiveTableOrderApplicationService activeTableOrderApplicationService;

    public QrOrderingV2Controller(ActiveTableOrderApplicationService activeTableOrderApplicationService) {
        this.activeTableOrderApplicationService = activeTableOrderApplicationService;
    }

    @PostMapping("/submit")
    public ApiResponse<QrOrderingSubmitResultDto> submit(@Valid @RequestBody QrOrderingSubmitRequest request) {
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
}
