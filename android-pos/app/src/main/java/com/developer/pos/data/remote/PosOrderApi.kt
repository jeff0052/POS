package com.developer.pos.data.remote

import com.developer.pos.data.remote.dto.ActiveOrderDto
import com.developer.pos.data.remote.dto.CashierSettlementResultDto
import com.developer.pos.data.remote.dto.CollectCashierSettlementRequestDto
import com.developer.pos.data.remote.dto.DailySummaryDto
import com.developer.pos.data.remote.dto.MerchantAdminOrderDto
import com.developer.pos.data.remote.dto.OrderStageTransitionDto
import com.developer.pos.data.remote.dto.ReplaceActiveOrderItemsRequestDto
import com.developer.pos.data.remote.dto.SalesSummaryDto
import com.developer.pos.data.remote.dto.SettlementPreviewDto
import com.developer.pos.data.remote.dto.StartVibeCashPaymentRequestDto
import com.developer.pos.data.remote.dto.VibeCashPaymentAttemptDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PosOrderApi {
    @GET("admin/orders?storeId=101")
    suspend fun getMerchantOrders(): ApiEnvelope<List<MerchantAdminOrderDto>>

    @GET("reports/daily-summary?storeId=101")
    suspend fun getDailySummary(): ApiEnvelope<DailySummaryDto>

    @GET("reports/sales-summary?storeId=101&merchantId=1")
    suspend fun getSalesSummary(): ApiEnvelope<SalesSummaryDto>

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

    @POST("stores/{storeId}/tables/{tableId}/payment/vibecash")
    suspend fun startVibeCashPayment(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long,
        @Body request: StartVibeCashPaymentRequestDto
    ): ApiEnvelope<VibeCashPaymentAttemptDto>

    @GET("stores/{storeId}/tables/{tableId}/payment/attempts/{paymentAttemptId}")
    suspend fun getVibeCashPaymentAttempt(
        @Path("storeId") storeId: Long,
        @Path("tableId") tableId: Long,
        @Path("paymentAttemptId") paymentAttemptId: String
    ): ApiEnvelope<VibeCashPaymentAttemptDto>
}
