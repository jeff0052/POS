package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.ConsumptionResult;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ConsumptionCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ConsumptionCalculationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JpaRecipeRepository recipeRepository;

    public ConsumptionCalculationService(JpaRecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    /**
     * Calculate ingredient consumption for one SKU order item.
     *
     * @param skuId             the ordered SKU
     * @param quantity          number of units ordered
     * @param optionSnapshotJson JSON from SubmittedOrderItemEntity.optionSnapshotJson (nullable)
     * @return list of consumption results per inventory item
     */
    public List<ConsumptionResult> calculate(Long skuId, int quantity, String optionSnapshotJson) {
        List<RecipeEntity> recipes = recipeRepository.findBySkuId(skuId);
        if (recipes.isEmpty()) return List.of();

        Set<Long> selectedOptionIds = parseSelectedOptionIds(optionSnapshotJson);
        BigDecimal qty = BigDecimal.valueOf(quantity);

        List<ConsumptionResult> results = new ArrayList<>();
        for (RecipeEntity recipe : recipes) {
            BigDecimal baseMultiplier = recipe.getBaseMultiplier() != null
                ? recipe.getBaseMultiplier() : BigDecimal.ONE;

            // Compute modifier effects (multiplier + extra)
            ModifierEffect effect = computeModifierEffect(recipe, selectedOptionIds, qty);

            // Base consumption × baseMultiplier × modifierMultiplier × quantity
            BigDecimal base = recipe.getConsumptionQty()
                .multiply(baseMultiplier)
                .multiply(effect.multiplier())
                .multiply(qty)
                .setScale(4, RoundingMode.HALF_UP);

            results.add(new ConsumptionResult(
                recipe.getInventoryItemId(),
                base.add(effect.extraQty()),
                recipe.getConsumptionUnit()
            ));
        }
        return results;
    }

    /**
     * Parse modifier rules and compute:
     * - multiplier adjustments (e.g. 大份 × 1.5) → returned as cumulative multiplier
     * - extra qty additions (e.g. 加辣 → +0.01kg) → returned as sum
     */
    private record ModifierEffect(BigDecimal multiplier, BigDecimal extraQty) {}

    private ModifierEffect computeModifierEffect(RecipeEntity recipe, Set<Long> selectedOptionIds,
                                                  BigDecimal qty) {
        if (selectedOptionIds.isEmpty() || recipe.getModifierConsumptionRules() == null) {
            return new ModifierEffect(BigDecimal.ONE, BigDecimal.ZERO);
        }
        try {
            List<Map<String, Object>> rules = MAPPER.readValue(
                recipe.getModifierConsumptionRules(),
                new TypeReference<>() {}
            );
            BigDecimal multiplier = BigDecimal.ONE;
            BigDecimal extra = BigDecimal.ZERO;
            for (Map<String, Object> rule : rules) {
                Long optionId = ((Number) rule.get("modifierOptionId")).longValue();
                if (!selectedOptionIds.contains(optionId)) continue;

                if (rule.containsKey("multiplier")) {
                    // Multiplier modifier: 大份 × 1.5
                    multiplier = multiplier.multiply(new BigDecimal(rule.get("multiplier").toString()));
                } else if (rule.containsKey("extraQty")) {
                    // Extra ingredient: 加辣 → +extraQty per unit
                    BigDecimal extraQty = new BigDecimal(rule.get("extraQty").toString());
                    extra = extra.add(extraQty.multiply(qty).setScale(4, RoundingMode.HALF_UP));
                }
            }
            return new ModifierEffect(multiplier, extra);
        } catch (Exception e) {
            log.warn("Failed to parse modifierConsumptionRules for recipe {}: {}",
                recipe.getId(), e.getMessage());
            return new ModifierEffect(BigDecimal.ONE, BigDecimal.ZERO);
        }
    }

    private Set<Long> parseSelectedOptionIds(String optionSnapshotJson) {
        if (optionSnapshotJson == null || optionSnapshotJson.isBlank()) return Set.of();
        try {
            List<Map<String, Object>> groups = MAPPER.readValue(
                optionSnapshotJson, new TypeReference<>() {}
            );
            Set<Long> ids = new HashSet<>();
            for (Map<String, Object> group : groups) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options =
                    (List<Map<String, Object>>) group.get("selectedOptions");
                if (options == null) continue;
                for (Map<String, Object> opt : options) {
                    ids.add(((Number) opt.get("optionId")).longValue());
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("Failed to parse optionSnapshotJson: {}", e.getMessage());
            return Set.of();
        }
    }
}
