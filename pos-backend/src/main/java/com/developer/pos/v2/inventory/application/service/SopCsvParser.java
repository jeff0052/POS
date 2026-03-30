package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.SopCsvRow;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SopCsvParser {

    public record RowError(int row, String field, String error) {}
    public record ParseResult(List<SopCsvRow> validRows, List<RowError> errors) {}

    public ParseResult parse(String csvContent) {
        List<SopCsvRow> valid = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String header = reader.readLine();
            if (header == null) return new ParseResult(valid, errors);

            String line;
            int rowNum = 0;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    errors.add(new RowError(rowNum, "row", "Too few columns, expected at least 4"));
                    continue;
                }

                Long skuId;
                try {
                    String raw = cols[0].trim();
                    if (raw.isEmpty()) { errors.add(new RowError(rowNum, "sku_id", "sku_id is required")); continue; }
                    skuId = Long.parseLong(raw);
                    if (skuId <= 0) { errors.add(new RowError(rowNum, "sku_id", "sku_id must be positive")); continue; }
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "sku_id", "sku_id must be a number")); continue;
                }

                String itemCode = cols[1].trim();
                if (itemCode.isEmpty()) { errors.add(new RowError(rowNum, "inventory_item_code", "inventory_item_code is required")); continue; }

                BigDecimal consumptionQty;
                try {
                    String raw = cols[2].trim();
                    if (raw.isEmpty()) { errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty is required")); continue; }
                    consumptionQty = new BigDecimal(raw);
                    if (consumptionQty.compareTo(BigDecimal.ZERO) <= 0) { errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty must be positive")); continue; }
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty must be a number")); continue;
                }

                String consumptionUnit = cols[3].trim();
                if (consumptionUnit.isEmpty()) { errors.add(new RowError(rowNum, "consumption_unit", "consumption_unit is required")); continue; }

                BigDecimal baseMultiplier = BigDecimal.ONE;
                if (cols.length > 4 && !cols[4].trim().isEmpty()) {
                    try { baseMultiplier = new BigDecimal(cols[4].trim()); }
                    catch (NumberFormatException e) { errors.add(new RowError(rowNum, "base_multiplier", "base_multiplier must be a number")); continue; }
                }

                String notes = cols.length > 5 ? cols[5].trim() : "";

                valid.add(new SopCsvRow(rowNum, skuId, itemCode, consumptionQty,
                    consumptionUnit, baseMultiplier, notes.isEmpty() ? null : notes));
            }
        } catch (Exception e) {
            errors.add(new RowError(0, "file", "Failed to read CSV: " + e.getMessage()));
        }

        return new ParseResult(valid, errors);
    }
}
