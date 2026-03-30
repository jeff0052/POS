package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface JpaRecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findBySkuId(Long skuId);
    List<RecipeEntity> findBySkuIdIn(java.util.Collection<Long> skuIds);
    // TODO: deleteBySkuId is global, not store-scoped. RecipeEntity lacks storeId field.
    // Store isolation relies on the caller only providing SKU IDs from the authenticated store.
    @Modifying
    void deleteBySkuId(Long skuId);
    List<RecipeEntity> findByInventoryItemId(Long inventoryItemId);
}
