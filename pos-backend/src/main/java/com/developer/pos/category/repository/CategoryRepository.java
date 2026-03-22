package com.developer.pos.category.repository;

import com.developer.pos.category.entity.CategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findByStoreIdAndDeletedOrderBySortOrderAsc(Long storeId, Integer deleted);
}
