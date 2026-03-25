package com.developer.pos.v2.report.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.report.application.dto.V2DailySummaryDto;
import com.developer.pos.v2.report.application.dto.V2SalesReportSummaryDto;
import com.developer.pos.v2.report.application.service.ReportReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/reports")
public class ReportV2Controller implements V2Api {

    private final ReportReadService reportReadService;

    public ReportV2Controller(ReportReadService reportReadService) {
        this.reportReadService = reportReadService;
    }

    @GetMapping("/daily-summary")
    public ApiResponse<V2DailySummaryDto> dailySummary(@RequestParam Long storeId) {
        return ApiResponse.success(reportReadService.getDailySummary(storeId));
    }

    @GetMapping("/sales-summary")
    public ApiResponse<V2SalesReportSummaryDto> salesSummary(
            @RequestParam(defaultValue = "101") Long storeId,
            @RequestParam(defaultValue = "1") Long merchantId
    ) {
        return ApiResponse.success(reportReadService.getSalesSummary(storeId, merchantId));
    }
}
