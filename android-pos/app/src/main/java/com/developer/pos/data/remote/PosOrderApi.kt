package com.developer.pos.data.remote

import com.developer.pos.data.remote.dto.ActiveOrderDto
import com.developer.pos.data.remote.dto.CashierSettlementResultDto
import com.developer.pos.data.remote.dto.OrderStageTransitionDto
import com.developer.pos.data.remote.dto.ReplaceActiveOrderItemsRequestDto
import com.developer.pos.data.remote.dto.SettlementPreviewDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PosOrderApi {
    @PUT("stores/{storeId}/tables/{tableId}/active-order/items")
    suspend fun replaceItems(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long,
        @Body request: ReplaceActiveOrderItemsRequestDto
    ): ApiEnvelope<ActiveOrderDto>

    @POST("stores/{storeId}/tables/{tableId}/active-order/{activeOrderId}/submit-to-kitchen")
    suspend fun submitToKitchen(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long,
        @Path("activeOrderId") activeOrderId: String
    ): ApiEnvelope<OrderStageTransitionDto>

    @POST("stores/{storeId}/tables/{tableId}/payment")
    suspend fun moveToPayment(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long
    ): ApiEnvelope<OrderStageTransitionDto>

    @GET("stores/{storeId}/tables/{tableId}/payment/preview")
    suspend fun getPaymentPreview(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long
    ): ApiEnvelope<SettlementPreviewDto>

    @POST("stores/{storeId}/tables/{tableId}/payment/collect")
    suspend fun collectPayment(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long,
        @Body request: CollectCashierSettlementRequestDto
    ): ApiEnvelope<CashierSettlementResultDto>
}
