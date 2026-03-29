package com.developer.pos.v2.audit.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.audit.application.dto.AuditTrailDto;
import com.developer.pos.v2.audit.application.service.AuditTrailService;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/audit")
public class AuditV2Controller implements V2Api {

    private final AuditTrailService auditTrailService;

    public AuditV2Controller(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/logs")
    public ApiResponse<Page<AuditTrailDto>> listAuditLogs(
            @RequestParam Long storeId,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditTrailService.listAuditLogs(storeId, targetType, page, size));
    }

    @GetMapping("/pending")
    public ApiResponse<Page<AuditTrailDto>> listPendingApprovals(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditTrailService.listPendingApprovals(storeId, page, size));
    }

    @PostMapping("/logs/{id}/approve")
    public ApiResponse<AuditTrailDto> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.getOrDefault("note", "") : "";
        return ApiResponse.success(auditTrailService.approve(id, note));
    }

    @PostMapping("/logs/{id}/reject")
    public ApiResponse<AuditTrailDto> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.getOrDefault("note", "") : "";
        return ApiResponse.success(auditTrailService.reject(id, note));
    }
}
