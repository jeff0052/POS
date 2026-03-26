package com.developer.pos.v2.gto.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.gto.application.dto.GenerateGtoBatchCommand;
import com.developer.pos.v2.gto.application.dto.GtoExportBatchDto;
import com.developer.pos.v2.gto.application.service.GtoExportApplicationService;
import com.developer.pos.v2.gto.interfaces.rest.request.GenerateGtoBatchRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class GtoV2Controller implements V2Api {

    private final GtoExportApplicationService gtoExportApplicationService;

    public GtoV2Controller(GtoExportApplicationService gtoExportApplicationService) {
        this.gtoExportApplicationService = gtoExportApplicationService;
    }

    @PostMapping("/gto/batches")
    public ApiResponse<GtoExportBatchDto> generateBatch(@RequestBody GenerateGtoBatchRequest request) {
        GenerateGtoBatchCommand command = new GenerateGtoBatchCommand(
                request.getMerchantId(),
                request.getStoreId(),
                request.getExportDate()
        );
        return ApiResponse.success(gtoExportApplicationService.generateBatch(command));
    }

    @GetMapping("/gto/batches/{batchId}")
    public ApiResponse<GtoExportBatchDto> getBatch(@PathVariable String batchId) {
        return ApiResponse.success(gtoExportApplicationService.getBatch(batchId));
    }

    @GetMapping("/gto/batches")
    public ApiResponse<Page<GtoExportBatchDto>> listBatches(
            @RequestParam Long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(gtoExportApplicationService.listBatches(merchantId, page, size));
    }

    @PostMapping("/gto/batches/{batchId}/retry")
    public ApiResponse<GtoExportBatchDto> retryBatch(@PathVariable String batchId) {
        return ApiResponse.success(gtoExportApplicationService.retryBatch(batchId));
    }
}
