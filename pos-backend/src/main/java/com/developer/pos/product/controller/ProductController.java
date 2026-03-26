package com.developer.pos.product.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.product.dto.ProductListResponse;
import com.developer.pos.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<ProductListResponse> list(@RequestParam(defaultValue = "1001") Long storeId) {
        return ApiResponse.success(productService.list(storeId));
    }
}
