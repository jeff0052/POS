package com.developer.pos.device.payment

interface PaymentService {
    suspend fun startPayment(orderNo: String, amountCents: Long): PaymentResult
    suspend fun queryPayment(orderNo: String): PaymentResult
    suspend fun cancelPayment(orderNo: String): PaymentResult
}

data class PaymentResult(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null,
    val sdkTradeNo: String? = null
)
