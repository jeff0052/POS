package com.developer.pos.data.remote

import com.developer.pos.data.remote.dto.QrMenuDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductApi {
    @GET("qr-ordering/menu")
    suspend fun getProducts(@Query("storeCode") storeCode: String = "1001"): ApiEnvelope<QrMenuDto>
}
