package com.developer.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val storeId: Long,
    val categoryId: Long?,
    val name: String,
    val barcode: String?,
    val priceCents: Long,
    val stockQty: Int,
    val enabled: Boolean
)
