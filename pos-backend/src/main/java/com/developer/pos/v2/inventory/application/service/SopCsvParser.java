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

    private static final int MAX_ROWS = 5000;

    public record RowError(int row, String field, String error) {}
    public record ParseResult(List<SopCsvRow> validRows, List<RowError> errors) {}

    public ParseResult parse(String csvContent) {
        List<SopCsvRow> valid = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String header = reader.readLine();
            if (header == null) return new ParseResult(valid, errors);

            // LIMITATION: This parser uses simple comma splitting and does NOT support:
            // - Fields with embedded commas (e.g., "Rice, white")
            // - Quoted fields or escaped quotes
            // - BOM markers
            // For production use, consider Apache Commons CSV or OpenCSV library.

            String line;
            int rowNum = 0;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (valid.size() + errors.size() >= MAX_ROWS) {
                    throw new IllegalArgumentException("CSV exceeds maximum of " + MAX_ROWS + " rows");
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    errors.add(new RowError(rowNum, "row", "Too few columns, expected at least 4"));
                    continue;
                }

                Long skuId;
                try {
                    String raw = sanitizeCsvCell(cols[0]);
                    if (raw.isEmpty()) { errors.add(new RowError(rowNum, "sku_id", "sku_id is required")); continue; }
                    skuId = Long.parseLong(raw);
                    if (skuId <= 0) { errors.add(new RowError(rowNum, "sku_id", "sku_id must be positive")); continue; }
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "sku_id", "sku_id must be a number")); continue;
                }

                String itemCode = sanitizeTextCell(cols[1]);
                if (itemCode.isEmpty()) { errors.add(new RowError(rowNum, "inventory_item_code", "inventory_item_code is required")); continue; }

                BigDecimal consumptionQty;
                try {
                    String raw = sanitizeCsvCell(cols[2]);
                    if (raw.isEmpty()) { errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty is required")); continue; }
                    consumptionQty = new BigDecimal(raw);
                    if (consumptionQty.compareTo(BigDecimal.ZERO) <= 0) { errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty must be positive")); continue; }
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "consumption_qty", "consumption_qty must be a number")); continue;
                }

                String consumptionUnit = sanitizeTextCell(cols[3]);
                if (consumptionUnit.isEmpty()) { errors.add(new RowError(rowNum, "consumption_unit", "consumption_unit is required")); continue; }

                BigDecimal baseMultiplier = BigDecimal.ONE;
                String baseMultiplierRaw = cols.length > 4 ? sanitizeCsvCell(cols[4]) : "";
                if (!baseMultiplierRaw.isEmpty()) {
                    try { baseMultiplier = new BigDecimal(baseMultiplierRaw); }
                    catch (NumberFormatException e) { errors.add(new RowError(rowNum, "base_multiplier", "base_multiplier must be a number")); continue; }
                }

                String notes = cols.length > 5 ? sanitizeTextCell(cols[5]) : "";

                valid.add(new SopCsvRow(rowNum, skuId, itemCode, consumptionQty,
                    consumptionUnit, baseMultiplier, notes.isEmpty() ? null : notes));
            }
        } catch (Exception e) {
            errors.add(new RowError(0, "file", "Failed to read CSV: " + e.getMessage()));
        }

        return new ParseResult(valid, errors);
    }

    /** Trim whitespace from a cell value. Numeric fields keep +/- signs intact. */
    private static String sanitizeCsvCell(String value) {
        if (value == null) return "";
        return value.trim();
    }

    /** Strip formula injection prefixes for text-only fields (OWASP CSV injection prevention). */
    private static String sanitizeTextCell(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return trimmed;
        while (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }
}
