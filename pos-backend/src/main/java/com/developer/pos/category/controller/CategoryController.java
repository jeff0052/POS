package com.developer.pos.category.controller;

import com.developer.pos.category.dto.CategoryListResponse;
import com.developer.pos.category.service.CategoryService;
import com.developer.pos.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<CategoryListResponse> list() {
        return ApiResponse.success(categoryService.list());
    }
}
