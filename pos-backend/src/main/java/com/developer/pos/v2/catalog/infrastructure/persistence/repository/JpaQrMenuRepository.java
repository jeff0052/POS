package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaQrMenuRepository extends JpaRepository<StoreSkuAvailabilityEntity, Long> {

    @Query(value = """
            SELECT
                s.id AS storeId,
                s.store_code AS storeCode,
                s.store_name AS storeName,
                c.id AS categoryId,
                c.category_code AS categoryCode,
                c.category_name AS categoryName,
                c.sort_order AS categorySortOrder,
                p.id AS productId,
                p.product_code AS productCode,
                p.product_name AS productName,
                k.id AS skuId,
                k.sku_code AS skuCode,
                k.sku_name AS skuName,
                k.base_price_cents AS unitPriceCents,
                p.image_id AS productImageId,
                k.image_id AS skuImageId
            FROM stores s
            JOIN product_categories c
              ON c.store_id = s.id
             AND c.is_active = 1
            JOIN products p
              ON p.store_id = s.id
             AND p.category_id = c.id
             AND p.product_status = 'ACTIVE'
            JOIN skus k
              ON k.product_id = p.id
             AND k.sku_status = 'ACTIVE'
            JOIN store_sku_availability a
              ON a.store_id = s.id
             AND a.sku_id = k.id
             AND a.is_available = 1
            WHERE s.store_code = :storeCode
              AND s.store_status = 'ACTIVE'
            ORDER BY c.sort_order ASC, c.id ASC, p.id ASC, k.id ASC
            """, nativeQuery = true)
    List<QrMenuProjection> findQrMenuByStoreCode(String storeCode);
}
