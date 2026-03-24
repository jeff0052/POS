package com.developer.pos.v2.promotion.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleSummaryDto;
import com.developer.pos.v2.promotion.application.service.PromotionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
public class PromotionV2Controller implements V2Api {

    private final PromotionApplicationService promotionApplicationService;

    public PromotionV2Controller(PromotionApplicationService promotionApplicationService) {
        this.promotionApplicationService = promotionApplicationService;
    }

    @GetMapping("/promotions")
    public ApiResponse<List<PromotionRuleSummaryDto>> listActiveRules(@RequestParam Long storeId) {
        return ApiResponse.success(promotionApplicationService.listActiveRules(storeId));
    }

    @PostMapping("/active-table-orders/{activeOrderId}/apply-best-promotion")
    public ApiResponse<PromotionEvaluationDto> applyBestPromotion(@PathVariable String activeOrderId) {
        return ApiResponse.success(promotionApplicationService.applyBestPromotion(activeOrderId));
    }
}
