package com.developer.pos.data.remote.dto

data class ProductDto(
    val id: Long,
    val storeId: Long,
    val categoryId: Long?,
    val name: String,
    val barcode: String?,
    val priceCents: Long,
    val stockQty: Int,
    val enabled: Boolean
)
