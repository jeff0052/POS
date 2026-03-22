package com.developer.pos.data.repository.impl

import com.developer.pos.data.local.dao.ProductDao
import com.developer.pos.data.local.entity.ProductEntity
import com.developer.pos.data.repository.ProductRepository
import com.developer.pos.domain.model.Product
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {
    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun seedIfEmpty() {
        if (productDao.count() > 0) return

        productDao.upsertAll(
            listOf(
                ProductEntity(1, 1, 1, "Coke", "001", 500, 100, true),
                ProductEntity(2, 1, 1, "Fried Rice", "002", 1800, 50, true),
                ProductEntity(3, 1, 2, "Noodles", "003", 1600, 40, true),
                ProductEntity(4, 1, 2, "Milk Tea", "004", 1200, 70, true)
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
