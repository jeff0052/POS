package com.developer.pos.data.remote.dto

data class MerchantAdminOrderItemDto(
    val productName: String,
    val quantity: Int,
    val amountCents: Long,
    val originalAmountCents: Long,
    val memberBenefitCents: Long,
    val promotionBenefitCents: Long,
    val gift: Boolean
)

data class MerchantAdminOrderDto(
    val orderId: String,
    val orderNo: String,
    val storeId: Long,
    val tableId: Long?,
    val tableCode: String?,
    val orderType: String,
    val orderStatus: String,
    val paymentMethod: String?,
    val createdAt: String,
    val memberName: String?,
    val memberTier: String?,
    val originalAmountCents: Long,
    val memberDiscountCents: Long,
    val promotionDiscountCents: Long,
    val payableAmountCents: Long,
    val giftItems: List<String>,
    val items: List<MerchantAdminOrderItemDto>
)

data class DailySummaryDto(
    val totalRevenueCents: Long,
    val orderCount: Long,
    val refundAmountCents: Long,
    val cashAmountCents: Long,
    val sdkPayAmountCents: Long
)

data class SalesSummaryDto(
    val totalSalesCents: Long,
    val totalDiscountCents: Long,
    val memberSalesCents: Long,
    val rechargeSalesCents: Long,
    val tableTurnoverRate: Double,
    val pendingGtoBatches: Long
)
