package com.developer.pos.product.service;

import com.developer.pos.product.dto.ProductDto;
import com.developer.pos.product.dto.ProductListResponse;
import com.developer.pos.product.entity.ProductEntity;
import com.developer.pos.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductListResponse list(Long storeId) {
        List<ProductDto> items = productRepository.findByStoreIdAndDeletedOrderByNameAsc(storeId, 0)
            .stream()
            .map(this::toDto)
            .toList();
        return new ProductListResponse(items, items.size());
    }

    private ProductDto toDto(ProductEntity entity) {
        return new ProductDto(
            entity.getId(),
            entity.getStoreId(),
            entity.getCategoryId(),
            entity.getName(),
            entity.getBarcode(),
            entity.getPriceCents(),
            entity.getStockQty(),
            entity.getStatus(),
            null
        );
    }
}
