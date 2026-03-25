package com.developer.pos.device.payment

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedPaymentService @Inject constructor(
    private val dcsPaymentService: DcsPaymentService,
    private val vibeCashPaymentService: VibeCashPaymentService,
    private val cashPaymentService: CashPaymentService
) : PaymentService {
    override suspend fun connect(paymentMethod: String): PaymentConnectionStatus {
        return delegate(paymentMethod).connect(paymentMethod)
    }

    override fun currentStatus(paymentMethod: String): PaymentConnectionStatus {
        return delegate(paymentMethod).currentStatus(paymentMethod)
    }

    override suspend fun startPayment(orderNo: String, amountCents: Long, paymentMethod: String): PaymentResult {
        return delegate(paymentMethod).startPayment(orderNo, amountCents, paymentMethod)
    }

    override suspend fun queryPayment(orderNo: String, paymentMethod: String): PaymentResult {
        return delegate(paymentMethod).queryPayment(orderNo, paymentMethod)
    }

    override suspend fun cancelPayment(orderNo: String, paymentMethod: String): PaymentResult {
        return delegate(paymentMethod).cancelPayment(orderNo, paymentMethod)
    }

    private fun delegate(paymentMethod: String): PaymentService = when {
        PaymentMethods.isCard(paymentMethod) -> dcsPaymentService
        PaymentMethods.isQr(paymentMethod) -> vibeCashPaymentService
        paymentMethod == PaymentMethods.CASH -> cashPaymentService
        else -> cashPaymentService
    }
}
