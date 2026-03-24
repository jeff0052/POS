package com.developer.pos.v2.promotion.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.promotion.application.dto.PromotionEvaluationDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleDetailDto;
import com.developer.pos.v2.promotion.application.dto.PromotionRuleSummaryDto;
import com.developer.pos.v2.promotion.application.dto.UpsertPromotionRuleDto;
import com.developer.pos.v2.promotion.application.service.PromotionApplicationService;
import com.developer.pos.v2.promotion.interfaces.rest.request.UpsertPromotionRuleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/promotions/{ruleId}")
    public ApiResponse<PromotionRuleDetailDto> getRule(@PathVariable Long ruleId) {
        return ApiResponse.success(promotionApplicationService.getRule(ruleId));
    }

    @PostMapping("/promotions")
    public ApiResponse<PromotionRuleDetailDto> createRule(@RequestBody UpsertPromotionRuleRequest request) {
        return ApiResponse.success(promotionApplicationService.createRule(toCommand(request)));
    }

    @PutMapping("/promotions/{ruleId}")
    public ApiResponse<PromotionRuleDetailDto> updateRule(
            @PathVariable Long ruleId,
            @RequestBody UpsertPromotionRuleRequest request
    ) {
        return ApiResponse.success(promotionApplicationService.updateRule(ruleId, toCommand(request)));
    }

    @PostMapping("/active-table-orders/{activeOrderId}/apply-best-promotion")
    public ApiResponse<PromotionEvaluationDto> applyBestPromotion(@PathVariable String activeOrderId) {
        return ApiResponse.success(promotionApplicationService.applyBestPromotion(activeOrderId));
    }

    private UpsertPromotionRuleDto toCommand(UpsertPromotionRuleRequest request) {
        return new UpsertPromotionRuleDto(
                request.getMerchantId(),
                request.getStoreId(),
                request.getRuleCode(),
                request.getRuleName(),
                request.getRuleType(),
                request.getRuleStatus(),
                request.getPriority() == null ? 100 : request.getPriority(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getConditionType(),
                request.getThresholdAmountCents(),
                request.getRewardType(),
                request.getDiscountAmountCents(),
                request.getGiftSkuId(),
                request.getGiftQuantity()
        );
    }
}
