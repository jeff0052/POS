# V2 SKU Data Model and API Contract

## 1. Purpose

这份文档把 SKU 需求从 requirements 推进到可开发基线。

范围包括：

- 数据模型建议
- 关键表
- 关键字段
- 关键 API contract

## 2. Core Tables

## 2.1 Catalog

- `categories`
- `products`
- `skus`

## 2.2 Attribute

- `attribute_groups`
- `attribute_values`
- `product_attribute_groups`
- `sku_attribute_groups`

## 2.3 Modifier

- `modifier_groups`
- `modifier_values`
- `product_modifier_groups`
- `sku_modifier_groups`

## 2.4 Combo

- `combos`
- `combo_slots`
- `combo_slot_allowed_skus`

## 2.5 Availability

- `store_sku_availability`

## 2.6 Order Snapshot

- `order_items`
- `order_item_attributes`
- `order_item_modifiers`

## 2.7 Kitchen

- `kitchen_items`
- `kitchen_item_events`

## 3. Key Field Suggestions

### products

- `product_id`
- `merchant_id`
- `category_id`
- `product_code`
- `product_name`
- `status`
- `image_url`
- `display_order`

### skus

- `sku_id`
- `product_id`
- `sku_code`
- `sku_name`
- `base_price_cents`
- `currency_code`
- `status`
- `is_default`
- `kitchen_station_code`

### attribute_groups

- `attribute_group_id`
- `group_code`
- `group_name`
- `selection_type`
- `min_select`
- `max_select`
- `is_required`
- `status`

### attribute_values

- `attribute_value_id`
- `attribute_group_id`
- `value_code`
- `value_name`
- `price_delta_cents`
- `is_default`
- `kitchen_label`
- `status`

### modifier_groups / modifier_values

与 attribute 结构类似，但额外可加入：

- `modifier_kind`
  - `ADD_ON`
  - `EXCLUSION`
  - `REPLACEMENT`
  - `UPGRADE`

### store_sku_availability

- `store_sku_availability_id`
- `store_id`
- `sku_id`
- `availability_status`
- `reason_code`
- `updated_at`

### order_items

- `order_item_id`
- `submitted_order_id`
- `sku_id`
- `product_name_snapshot`
- `sku_name_snapshot`
- `unit_price_snapshot`
- `attribute_snapshot_json`
- `modifier_snapshot_json`
- `remark_snapshot`
- `quantity`
- `line_total_cents`

## 4. Read APIs

## 4.1 Merchant Admin

### GET /api/v2/catalog/categories

支持：

- `merchantId`
- `status`

### GET /api/v2/catalog/products

支持：

- `merchantId`
- `categoryId`
- `status`
- `keyword`

### GET /api/v2/catalog/skus

支持：

- `merchantId`
- `productId`
- `status`
- `keyword`

### GET /api/v2/catalog/attributes

支持：

- `merchantId`
- `scopeType`
- `scopeId`

### GET /api/v2/catalog/modifiers

支持：

- `merchantId`
- `scopeType`
- `scopeId`

### GET /api/v2/catalog/combos

支持：

- `merchantId`
- `status`

### GET /api/v2/catalog/store-availability

支持：

- `storeId`
- `skuId`
- `availabilityStatus`

## 4.2 POS / QR Menu

### GET /api/v2/qr-ordering/menu?storeCode=1001

返回建议结构：

```json
{
  "storeId": 101,
  "storeCode": "1001",
  "categories": [
    {
      "categoryId": 11,
      "categoryName": "Beverages",
      "products": [
        {
          "productId": 201,
          "productName": "Milk Tea",
          "imageUrl": "https://...",
          "skus": [
            {
              "skuId": 301,
              "skuName": "Milk Tea Regular",
              "basePriceCents": 450,
              "attributes": [],
              "modifiers": []
            }
          ]
        }
      ]
    }
  ]
}
```

## 5. Write APIs

## 5.1 Categories

- `POST /api/v2/catalog/categories`
- `PUT /api/v2/catalog/categories/{categoryId}`

## 5.2 Products

- `POST /api/v2/catalog/products`
- `PUT /api/v2/catalog/products/{productId}`

## 5.3 SKUs

- `POST /api/v2/catalog/skus`
- `PUT /api/v2/catalog/skus/{skuId}`

请求建议：

```json
{
  "productId": 201,
  "skuCode": "milk-tea-regular",
  "skuName": "Milk Tea Regular",
  "basePriceCents": 450,
  "status": "ACTIVE",
  "isDefault": true,
  "attributeGroupIds": [601, 602],
  "modifierGroupIds": [701, 702]
}
```

## 5.4 Attributes

- `POST /api/v2/catalog/attribute-groups`
- `PUT /api/v2/catalog/attribute-groups/{attributeGroupId}`
- `POST /api/v2/catalog/attribute-values`
- `PUT /api/v2/catalog/attribute-values/{attributeValueId}`

## 5.5 Modifiers

- `POST /api/v2/catalog/modifier-groups`
- `PUT /api/v2/catalog/modifier-groups/{modifierGroupId}`
- `POST /api/v2/catalog/modifier-values`
- `PUT /api/v2/catalog/modifier-values/{modifierValueId}`

## 5.6 Combos

- `POST /api/v2/catalog/combos`
- `PUT /api/v2/catalog/combos/{comboId}`
- `POST /api/v2/catalog/combo-slots`
- `PUT /api/v2/catalog/combo-slots/{slotId}`

## 5.7 Store Availability

- `PUT /api/v2/catalog/store-availability`

请求建议：

```json
{
  "storeId": 101,
  "skuId": 301,
  "availabilityStatus": "OUT_OF_STOCK",
  "reasonCode": "SOLD_OUT_TODAY"
}
```

## 6. Ordering Payload Rules

点单 payload 应明确分开：

- `attributes`
- `modifiers`
- `remark`

建议：

```json
{
  "skuId": 301,
  "quantity": 1,
  "attributes": [
    { "attributeGroupId": 601, "attributeValueId": 9101 }
  ],
  "modifiers": [
    { "modifierValueId": 9201, "quantity": 1 }
  ],
  "remark": "Pack separately"
}
```

## 7. Validation Rules

提交订单时必须校验：

- Product status
- SKU status
- Store availability
- required attribute present
- max select not exceeded
- incompatible combinations blocked
- modifier quantity legal

## 8. Error Codes

- `PRODUCT_NOT_ACTIVE`
- `SKU_NOT_ACTIVE`
- `SKU_NOT_AVAILABLE`
- `REQUIRED_ATTRIBUTE_MISSING`
- `ATTRIBUTE_SELECTION_EXCEEDED`
- `ATTRIBUTE_COMBINATION_NOT_ALLOWED`
- `MODIFIER_NOT_ALLOWED`
- `COMBO_SLOT_SELECTION_INVALID`

## 9. Implementation Priority

### Phase 1

- categories
- products
- skus
- store availability
- attribute group/value
- order snapshot

### Phase 2

- modifiers
- combo slots
- kitchen item integration

### Phase 3

- batch admin operations
- advanced pricing rules
- AI-ready catalog recommendations

## 10. Final Summary

这份 contract 的目的不是一次性做很复杂，而是把未来会复杂的地方先建对：

- 交易对象是 SKU
- 点单维度通过 attribute / modifier 表达
- 套餐槽位单独建模
- 历史订单只看 snapshot
- 可售状态与履约状态严格分层
