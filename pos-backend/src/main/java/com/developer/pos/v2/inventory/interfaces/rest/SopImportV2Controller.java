package com.developer.pos.v2.inventory.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.inventory.application.dto.SopImportBatchDto;
import com.developer.pos.v2.inventory.application.service.SopImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class SopImportV2Controller implements V2Api {

    private final SopImportService sopImportService;

    public SopImportV2Controller(SopImportService sopImportService) {
        this.sopImportService = sopImportService;
    }

    @PostMapping("/stores/{storeId}/sop-import")
    public ApiResponse<SopImportBatchDto> importSop(
            @PathVariable Long storeId,
            @RequestParam("file") MultipartFile file) {
        try {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "sop.csv";
            return ApiResponse.success(sopImportService.importCsv(storeId, fileName, csvContent));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    @GetMapping("/stores/{storeId}/sop-import")
    public ApiResponse<List<SopImportBatchDto>> listBatches(@PathVariable Long storeId) {
        return ApiResponse.success(sopImportService.listBatches(storeId));
    }
}
