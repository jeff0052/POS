package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.inventory.application.dto.OcrLineItem;
import com.developer.pos.v2.inventory.application.dto.OcrMatchedItem;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
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
class OcrAutoMatchServiceTest {

    @Mock JpaInventoryItemRepository itemRepository;

    private OcrAutoMatchService buildService() {
        return new OcrAutoMatchService(itemRepository);
    }

    private InventoryItemEntity makeItem(Long id, Long storeId, String itemName) {
        try {
            Constructor<InventoryItemEntity> ctor = InventoryItemEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            InventoryItemEntity item = ctor.newInstance();
            setField(item, "id", id);
            setField(item, "storeId", storeId);
            setField(item, "itemName", itemName);
            return item;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @Test
    void exactNameMatch_returnsHighConfidence() {
        InventoryItemEntity beef = makeItem(100L, 1L, "牛腩");
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(1L, "ACTIVE"))
            .thenReturn(List.of(beef));

        OcrLineItem line = new OcrLineItem("牛腩 5kg", new BigDecimal("5.0"), "kg", 8000L, 40000L);
        List<OcrMatchedItem> results = buildService().match(1L, List.of(line));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedInventoryItemId()).isEqualTo(100L);
        assertThat(results.get(0).matchedItemName()).isEqualTo("牛腩");
        assertThat(results.get(0).confidence()).isGreaterThanOrEqualTo(new BigDecimal("0.80"));
    }

    @Test
    void noMatch_returnsNullItemId() {
        InventoryItemEntity beef = makeItem(100L, 1L, "牛腩");
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(1L, "ACTIVE"))
            .thenReturn(List.of(beef));

        OcrLineItem line = new OcrLineItem("洗洁精", new BigDecimal("2.0"), "bottle", 1500L, 3000L);
        List<OcrMatchedItem> results = buildService().match(1L, List.of(line));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedInventoryItemId()).isNull();
    }

    @Test
    void partialMatch_containsSubstring() {
        InventoryItemEntity chili = makeItem(200L, 1L, "辣椒");
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(1L, "ACTIVE"))
            .thenReturn(List.of(chili));

        OcrLineItem line = new OcrLineItem("新鲜辣椒 2kg", new BigDecimal("2.0"), "kg", 5000L, 10000L);
        List<OcrMatchedItem> results = buildService().match(1L, List.of(line));

        assertThat(results.get(0).matchedInventoryItemId()).isEqualTo(200L);
    }

    @Test
    void multipleItems_matchesBest() {
        InventoryItemEntity beef = makeItem(100L, 1L, "牛腩");
        InventoryItemEntity rice = makeItem(200L, 1L, "大米");
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(1L, "ACTIVE"))
            .thenReturn(List.of(beef, rice));

        List<OcrLineItem> lines = List.of(
            new OcrLineItem("牛腩 5kg", new BigDecimal("5.0"), "kg", 8000L, 40000L),
            new OcrLineItem("大米 10kg", new BigDecimal("10.0"), "kg", 3000L, 30000L)
        );
        List<OcrMatchedItem> results = buildService().match(1L, lines);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).matchedInventoryItemId()).isEqualTo(100L);
        assertThat(results.get(1).matchedInventoryItemId()).isEqualTo(200L);
    }

    @Test
    void emptyInventory_allUnmatched() {
        when(itemRepository.findByStoreIdAndItemStatusOrderByItemNameAsc(1L, "ACTIVE"))
            .thenReturn(List.of());

        OcrLineItem line = new OcrLineItem("牛腩 5kg", new BigDecimal("5.0"), "kg", 8000L, 40000L);
        List<OcrMatchedItem> results = buildService().match(1L, List.of(line));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedInventoryItemId()).isNull();
    }
}
