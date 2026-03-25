package com.developer.pos.data.remote.dto

data class ReplaceActiveOrderItemsRequestDto(
    val orderSource: String,
    val memberId: Long?,
    val items: List<ReplaceActiveOrderItemDto>
)

data class ReplaceActiveOrderItemDto(
    val skuId: Long,
    val skuCode: String,
    val skuName: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val remark: String?
)

data class ActiveOrderDto(
    val activeOrderId: String,
    val orderNo: String,
    val tableCode: String,
    val orderSource: String,
    val status: String,
    val memberId: Long?,
    val originalAmountCents: Long,
    val memberDiscountCents: Long,
    val promotionDiscountCents: Long,
    val payableAmountCents: Long
)

data class OrderStageTransitionDto(
    val activeOrderId: String,
    val status: String
)

data class SettlementPreviewDto(
    val activeOrderId: String,
    val status: String,
    val member: SettlementMemberDto?,
    val pricing: SettlementPricingDto,
    val giftItems: List<SettlementGiftItemDto>
)

data class SettlementMemberDto(
    val memberId: Long,
    val memberName: String,
    val memberTier: String
)

data class SettlementPricingDto(
    val originalAmountCents: Long,
    val memberDiscountCents: Long,
    val promotionDiscountCents: Long,
    val payableAmountCents: Long
)

data class SettlementGiftItemDto(
    val skuCode: String,
    val skuName: String,
    val quantity: Int
)

data class CollectCashierSettlementRequestDto(
    val cashierId: Long,
    val paymentMethod: String,
    val collectedAmountCents: Long
)

data class CashierSettlementResultDto(
    val activeOrderId: String,
    val settlementNo: String,
    val finalStatus: String,
    val payableAmountCents: Long,
    val collectedAmountCents: Long
)

data class StartVibeCashPaymentRequestDto(
    val paymentScheme: String
)

data class VibeCashPaymentAttemptDto(
    val paymentAttemptId: String,
    val provider: String,
    val paymentMethod: String,
    val paymentScheme: String,
    val attemptStatus: String,
    val providerStatus: String,
    val providerPaymentId: String?,
    val checkoutUrl: String?,
    val settlementAmountCents: Long,
    val currencyCode: String
)
