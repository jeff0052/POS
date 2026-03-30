package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.SopCsvRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class SopCsvParserTest {

    private final SopCsvParser parser = new SopCsvParser();

    @Test
    void validCsv_parsesAllRows() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n" +
            "10,BEEF-001,0.2000,kg,1.50,Beef\n" +
            "20,RICE-001,0.3000,kg,,Rice\n";
        SopCsvParser.ParseResult result = parser.parse(csv);

        assertThat(result.validRows()).hasSize(2);
        assertThat(result.errors()).isEmpty();

        SopCsvRow first = result.validRows().get(0);
        assertThat(first.skuId()).isEqualTo(10L);
        assertThat(first.inventoryItemCode()).isEqualTo("BEEF-001");
        assertThat(first.consumptionQty()).isEqualByComparingTo("0.2000");
        assertThat(first.consumptionUnit()).isEqualTo("kg");
        assertThat(first.baseMultiplier()).isEqualByComparingTo("1.50");
        assertThat(first.notes()).isEqualTo("Beef");

        SopCsvRow second = result.validRows().get(1);
        assertThat(second.baseMultiplier()).isEqualByComparingTo("1.00");
    }

    @Test
    void missingSkuId_reportsError() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n" +
            ",BEEF-001,0.2000,kg,,\n";
        SopCsvParser.ParseResult result = parser.parse(csv);
        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).contains("sku_id");
    }

    @Test
    void negativeConsumptionQty_reportsError() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n" +
            "10,BEEF-001,-0.5,kg,,\n";
        SopCsvParser.ParseResult result = parser.parse(csv);
        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).contains("consumption_qty");
    }

    @Test
    void mixedValidAndInvalid_separatesCorrectly() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n" +
            "10,BEEF-001,0.2000,kg,,Good\n" +
            ",RICE-001,0.3000,kg,,Bad\n" +
            "20,CHILI-001,0.0100,kg,,Good\n";
        SopCsvParser.ParseResult result = parser.parse(csv);
        assertThat(result.validRows()).hasSize(2);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).row()).isEqualTo(2);
    }

    @Test
    void emptyCsv_returnsEmpty() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n";
        SopCsvParser.ParseResult result = parser.parse(csv);
        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void malformedNumber_reportsError() {
        String csv = "sku_id,inventory_item_code,consumption_qty,consumption_unit,base_multiplier,notes\n" +
            "abc,BEEF-001,0.2000,kg,,\n";
        SopCsvParser.ParseResult result = parser.parse(csv);
        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).contains("sku_id");
    }
}
