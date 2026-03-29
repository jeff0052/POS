package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.ConsumptionResult;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumptionCalculationServiceTest {

    @Mock JpaRecipeRepository recipeRepository;

    private ConsumptionCalculationService buildService() {
        return new ConsumptionCalculationService(recipeRepository);
    }

    // --- Reflection helpers ---
    private RecipeEntity makeRecipe(Long id, Long skuId, Long inventoryItemId,
                                     BigDecimal consumptionQty, String consumptionUnit,
                                     BigDecimal baseMultiplier, String modifierRulesJson) {
        try {
            Constructor<RecipeEntity> ctor = RecipeEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            RecipeEntity r = ctor.newInstance();
            setField(r, "id", id);
            setField(r, "skuId", skuId);
            setField(r, "inventoryItemId", inventoryItemId);
            setField(r, "consumptionQty", consumptionQty);
            setField(r, "consumptionUnit", consumptionUnit);
            setField(r, "baseMultiplier", baseMultiplier);
            setField(r, "modifierConsumptionRules", modifierRulesJson);
            return r;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @Test
    void baseRecipe_noModifiers_returnsBaseConsumption() {
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, null);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        List<ConsumptionResult> results = buildService().calculate(10L, 3, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).inventoryItemId()).isEqualTo(100L);
        assertThat(results.get(0).qty()).isEqualByComparingTo("0.6000");
        assertThat(results.get(0).unit()).isEqualTo("kg");
    }

    @Test
    void baseRecipe_withBaseMultiplier_appliesMultiplier() {
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", new BigDecimal("1.50"), null);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        List<ConsumptionResult> results = buildService().calculate(10L, 2, null);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.6000");
    }

    @Test
    void modifierExtraQty_addsExtraConsumption() {
        String rules = "[{\"modifierOptionId\": 101, \"extraQty\": 0.01, \"unit\": \"kg\"}]";
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, rules);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":101}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 2, optionJson);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).qty()).isEqualByComparingTo("0.4200");
    }

    @Test
    void modifierNotSelected_noExtraConsumption() {
        String rules = "[{\"modifierOptionId\": 101, \"extraQty\": 0.01, \"unit\": \"kg\"}]";
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, rules);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":102}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 1, optionJson);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.2000");
    }

    @Test
    void multipleRecipes_multipleIngredients() {
        RecipeEntity beef = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, null);
        RecipeEntity rice = makeRecipe(2L, 10L, 200L,
            new BigDecimal("0.3000"), "kg", BigDecimal.ONE, null);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(beef, rice));

        List<ConsumptionResult> results = buildService().calculate(10L, 2, null);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ConsumptionResult::inventoryItemId)
            .containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void noRecipes_returnsEmpty() {
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of());

        List<ConsumptionResult> results = buildService().calculate(10L, 5, null);

        assertThat(results).isEmpty();
    }

    @Test
    void nullBaseMultiplier_defaultsToOne() {
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", null, null);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        List<ConsumptionResult> results = buildService().calculate(10L, 1, null);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.2000");
    }

    @Test
    void multipleModifierRules_onlyMatchingApplied() {
        String rules = "[{\"modifierOptionId\":101,\"extraQty\":0.01,\"unit\":\"kg\"},{\"modifierOptionId\":102,\"extraQty\":0.05,\"unit\":\"kg\"}]";
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, rules);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":101}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 1, optionJson);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.2100");
    }

    @Test
    void multiplierModifier_scalesBaseConsumption() {
        String rules = "[{\"modifierOptionId\":201,\"multiplier\":1.5}]";
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, rules);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":201}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 2, optionJson);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.6000");
    }

    @Test
    void multiplierAndExtra_combined() {
        String rules = "[{\"modifierOptionId\":201,\"multiplier\":1.5},{\"modifierOptionId\":101,\"extraQty\":0.01,\"unit\":\"kg\"}]";
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, rules);
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":201},{\"optionId\":101}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 1, optionJson);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.3100");
    }

    @Test
    void malformedModifierRulesJson_gracefulFallback() {
        RecipeEntity recipe = makeRecipe(1L, 10L, 100L,
            new BigDecimal("0.2000"), "kg", BigDecimal.ONE, "NOT VALID JSON");
        when(recipeRepository.findBySkuId(10L)).thenReturn(List.of(recipe));

        String optionJson = "[{\"modifierGroupId\":1,\"selectedOptions\":[{\"optionId\":101}]}]";

        List<ConsumptionResult> results = buildService().calculate(10L, 1, optionJson);

        assertThat(results.get(0).qty()).isEqualByComparingTo("0.2000");
    }
}
