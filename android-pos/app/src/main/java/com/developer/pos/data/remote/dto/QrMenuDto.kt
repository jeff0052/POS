package com.developer.pos.data.remote.dto

data class QrMenuDto(
    val storeId: Long,
    val storeCode: String,
    val storeName: String,
    val categories: List<QrMenuCategoryDto>
)

data class QrMenuCategoryDto(
    val categoryId: Long,
    val categoryCode: String,
    val categoryName: String,
    val items: List<QrMenuItemDto>
)

data class QrMenuItemDto(
    val skuId: Long,
    val skuCode: String,
    val skuName: String,
    val unitPriceCents: Long,
    val categoryId: Long
)
