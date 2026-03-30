package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.OcrLineItem;
import com.developer.pos.v2.inventory.application.dto.OcrMatchedItem;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
// NOTE: Recipe queries use findBySkuId which is not store-scoped. Store isolation is
// inherited from the SKU/catalog layer — recipes are only accessible for SKUs belonging
// to the authenticated store. Direct recipe queries should not be exposed without store filtering.
public class OcrAutoMatchService {

    private static final BigDecimal MATCH_THRESHOLD = new BigDecimal("0.60");

    private final JpaInventoryItemRepository itemRepository;

    public OcrAutoMatchService(JpaInventoryItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Loads all active items for the store on each call. For stores with very large inventories
    // (10,000+ items), consider adding a cache or paginated matching strategy.
    // O(N*M) matching — acceptable for typical restaurant scale (50-200 items x 5-30 OCR lines)
    public List<OcrMatchedItem> match(Long storeId, List<OcrLineItem> ocrLines) {
        List<InventoryItemEntity> items = itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(storeId, "ACTIVE");
        List<OcrMatchedItem> results = new ArrayList<>();

        for (OcrLineItem line : ocrLines) {
            OcrMatchedItem best = findBestMatch(line, items);
            results.add(best);
        }
        return results;
    }

    private OcrMatchedItem findBestMatch(OcrLineItem line, List<InventoryItemEntity> items) {
        String ocrText = line.rawText().toLowerCase().trim();
        Long bestId = null;
        String bestName = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        for (InventoryItemEntity item : items) {
            String itemName = item.getItemName().toLowerCase().trim();
            BigDecimal score = computeSimilarity(ocrText, itemName);
            if (score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestId = item.getId();
                bestName = item.getItemName();
            }
        }

        if (bestScore.compareTo(MATCH_THRESHOLD) < 0) {
            bestId = null;
            bestName = null;
            bestScore = BigDecimal.ZERO;
        }

        return new OcrMatchedItem(
            line.rawText(), bestId, bestName,
            bestScore.setScale(2, RoundingMode.HALF_UP),
            line.qty(), line.unit(), line.unitPriceCents(), line.lineTotalCents()
        );
    }

    // Known limitation: character-bag overlap may produce false positives for CJK text. Consider Levenshtein for production.
    static BigDecimal computeSimilarity(String ocrText, String itemName) {
        if (ocrText.length() < 2) return BigDecimal.ZERO;
        if (ocrText.contains(itemName)) {
            BigDecimal ratio = BigDecimal.valueOf(itemName.length())
                .divide(BigDecimal.valueOf(ocrText.length()), 4, RoundingMode.HALF_UP);
            return new BigDecimal("0.80").add(ratio.multiply(new BigDecimal("0.20")));
        }
        if (itemName.contains(ocrText)) {
            return new BigDecimal("0.70");
        }
        long common = ocrText.chars()
            .filter(c -> itemName.indexOf(c) >= 0)
            .count();
        BigDecimal overlap = BigDecimal.valueOf(common)
            .divide(BigDecimal.valueOf(Math.max(ocrText.length(), itemName.length())), 4, RoundingMode.HALF_UP);
        return overlap;
    }
}
