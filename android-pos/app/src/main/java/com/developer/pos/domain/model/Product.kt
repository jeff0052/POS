package com.developer.pos.domain.model

data class Product(
    val id: Long,
    val storeId: Long,
    val categoryId: Long?,
    val name: String,
    val barcode: String?,
    val priceCents: Long,
    val stockQty: Int,
    val enabled: Boolean
)
