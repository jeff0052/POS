package com.developer.pos.report.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.report.dto.DailySummaryDto;
import com.developer.pos.report.dto.SalesReportSummaryDto;
import com.developer.pos.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily-summary")
    public ApiResponse<DailySummaryDto> dailySummary() {
        return ApiResponse.success(reportService.dailySummary());
    }

    @GetMapping("/sales-summary")
    public ApiResponse<SalesReportSummaryDto> salesSummary() {
        return ApiResponse.success(reportService.salesSummary());
    }
}
