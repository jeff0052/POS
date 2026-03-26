package com.developer.pos.category.service;

import com.developer.pos.category.dto.CategoryDto;
import com.developer.pos.category.dto.CategoryListResponse;
import com.developer.pos.category.entity.CategoryEntity;
import com.developer.pos.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryListResponse list(Long storeId) {
        List<CategoryDto> items = categoryRepository.findByStoreIdAndDeletedOrderBySortOrderAsc(storeId, 0)
            .stream()
            .map(this::toDto)
            .toList();
        return new CategoryListResponse(items);
    }

    private CategoryDto toDto(CategoryEntity entity) {
        return new CategoryDto(
            entity.getId(),
            entity.getStoreId(),
            entity.getName(),
            entity.getSortOrder(),
            entity.getStatus()
        );
    }
}
