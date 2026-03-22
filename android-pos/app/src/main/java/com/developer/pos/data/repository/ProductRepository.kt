package com.developer.pos.data.repository

import com.developer.pos.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    suspend fun seedIfEmpty()
}
