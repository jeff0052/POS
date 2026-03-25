package com.developer.pos.ui.model

data class BackofficeOrderItem(
    val productName: String,
    val quantity: Int,
    val amountCents: Long,
    val originalAmountCents: Long,
    val memberBenefitCents: Long,
    val promotionBenefitCents: Long,
    val gift: Boolean
)

data class BackofficeOrder(
    val orderId: String,
    val orderNo: String,
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
    val items: List<BackofficeOrderItem>
)

data class BackofficeDashboard(
    val totalRevenueCents: Long = 0L,
    val orderCount: Long = 0L,
    val refundAmountCents: Long = 0L,
    val rechargeSalesCents: Long = 0L,
    val totalDiscountCents: Long = 0L,
    val pendingQrTables: List<String> = emptyList()
)
