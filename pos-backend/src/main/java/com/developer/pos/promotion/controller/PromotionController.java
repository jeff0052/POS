package com.developer.pos.promotion.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.promotion.dto.PromotionRuleDto;
import com.developer.pos.promotion.service.PromotionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ApiResponse<List<PromotionRuleDto>> listRules() {
        return ApiResponse.success(promotionService.listRules());
    }
}
