package com.developer.pos.product.dto;

import java.util.List;

public record ProductListResponse(
    List<ProductDto> list,
    Integer total
) {
}
