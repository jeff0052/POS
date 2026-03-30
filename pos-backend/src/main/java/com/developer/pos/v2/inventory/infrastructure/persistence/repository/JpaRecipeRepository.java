package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface JpaRecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findBySkuId(Long skuId);
    List<RecipeEntity> findBySkuIdIn(java.util.Collection<Long> skuIds);
    @Modifying
    void deleteBySkuId(Long skuId);
    List<RecipeEntity> findByInventoryItemId(Long inventoryItemId);
}
