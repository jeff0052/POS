package com.developer.pos.product.repository;

import com.developer.pos.product.entity.ProductEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findByStoreIdAndDeletedOrderByNameAsc(Long storeId, Integer deleted);
}
