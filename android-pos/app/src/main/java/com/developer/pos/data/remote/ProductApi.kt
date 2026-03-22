package com.developer.pos.data.remote

import com.developer.pos.data.remote.dto.ProductDto
import retrofit2.http.GET

interface ProductApi {
    @GET("products")
    suspend fun getProducts(): List<ProductDto>
}
