package com.developer.pos.device.payment

interface PaymentService {
    suspend fun connect(paymentMethod: String): PaymentConnectionStatus
    fun currentStatus(paymentMethod: String): PaymentConnectionStatus
    suspend fun startPayment(orderNo: String, amountCents: Long, paymentMethod: String): PaymentResult
    suspend fun queryPayment(orderNo: String, paymentMethod: String): PaymentResult
    suspend fun cancelPayment(orderNo: String, paymentMethod: String): PaymentResult
}

data class PaymentConnectionStatus(
    val available: Boolean,
    val connected: Boolean,
    val providerName: String,
    val message: String
)

data class PaymentResult(
    val success: Boolean,
    val pending: Boolean = false,
    val code: String? = null,
    val message: String? = null,
    val sdkTradeNo: String? = null,
    val actionUrl: String? = null
)

object PaymentMethods {
    const val CASH = "CASH"
    const val CARD_TERMINAL = "CARD_TERMINAL"
    const val WECHAT_QR = "WECHAT_QR"
    const val ALIPAY_QR = "ALIPAY_QR"
    const val PAYNOW_QR = "PAYNOW_QR"

    fun isCard(method: String): Boolean = method == CARD_TERMINAL

    fun isQr(method: String): Boolean = method == WECHAT_QR || method == ALIPAY_QR || method == PAYNOW_QR

    fun providerName(method: String): String = when {
        isCard(method) -> "DCS"
        isQr(method) -> "VibeCash"
        method == CASH -> "Cash"
        else -> "Unknown"
    }

    fun displayName(method: String): String = when (method) {
        CASH -> "Cash"
        CARD_TERMINAL -> "Card Terminal"
        WECHAT_QR -> "WeChat QR"
        ALIPAY_QR -> "Alipay QR"
        PAYNOW_QR -> "PayNow QR"
        else -> method
    }
}
