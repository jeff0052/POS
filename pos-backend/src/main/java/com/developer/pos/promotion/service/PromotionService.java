package com.developer.pos.promotion.service;

import com.developer.pos.promotion.dto.PromotionRuleDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromotionService {

    public List<PromotionRuleDto> listRules() {
        return List.of(
            new PromotionRuleDto(1L, "Lunch full reduction", "FULL_REDUCTION", "ACTIVE", "Spend CNY 100 save CNY 10", 10),
            new PromotionRuleDto(2L, "Gold member fried rice price", "MEMBER_PRICE", "ACTIVE", "Gold members get CNY 18 fried rice", 20),
            new PromotionRuleDto(3L, "Recharge bonus 500+80", "RECHARGE_BONUS", "ACTIVE", "Recharge CNY 500 get CNY 80 bonus", 30)
        );
    }
}
