package com.developer.pos.data.repository.impl

import com.developer.pos.data.local.dao.ProductDao
import com.developer.pos.data.local.entity.ProductEntity
import com.developer.pos.data.remote.ProductApi
import com.developer.pos.data.repository.ProductRepository
import com.developer.pos.domain.model.Product
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val productApi: ProductApi
) : ProductRepository {
    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun seedIfEmpty() {
        if (productDao.count() > 0) return

        val remoteItems = runCatching { productApi.getProducts().data }
            .getOrNull()
            ?.categories
            ?.flatMap { category ->
                category.items.map { item ->
                    ProductEntity(
                        id = item.skuId,
                        storeId = 101,
                        categoryId = category.categoryId,
                        name = item.skuName,
                        barcode = item.skuCode,
                        priceCents = item.unitPriceCents,
                        stockQty = 999,
                        enabled = true
                    )
                }
            }

        productDao.upsertAll(
            remoteItems?.takeIf { it.isNotEmpty() } ?: listOf(
                ProductEntity(401, 101, 301, "Fried Rice", "fried-rice-default", 1250, 999, true),
                ProductEntity(402, 101, 301, "Black Pepper Beef Rice", "black-pepper-beef-rice-default", 10, 999, true),
                ProductEntity(403, 101, 302, "Crispy Chicken Bites", "crispy-chicken-bites-default", 1600, 999, true),
                ProductEntity(404, 101, 303, "White Peach Soda", "white-peach-soda-default", 1200, 999, true)
            )
        )
    }
}

private fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        storeId = storeId,
        categoryId = categoryId,
        name = name,
        barcode = barcode,
        priceCents = priceCents,
        stockQty = stockQty,
        enabled = enabled
    )
}
