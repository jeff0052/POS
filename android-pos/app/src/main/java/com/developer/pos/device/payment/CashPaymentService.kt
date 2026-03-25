package com.developer.pos.device.payment

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CashPaymentService @Inject constructor() : PaymentService {
    private val status = PaymentConnectionStatus(
        available = true,
        connected = true,
        providerName = "Cash",
        message = "Cash collection ready"
    )

    override suspend fun connect(paymentMethod: String): PaymentConnectionStatus = withContext(Dispatchers.IO) {
        status
    }

    override fun currentStatus(paymentMethod: String): PaymentConnectionStatus = status

    override suspend fun startPayment(orderNo: String, amountCents: Long, paymentMethod: String): PaymentResult =
        withContext(Dispatchers.IO) {
            PaymentResult(
                success = true,
                code = null,
                message = "Cash collection confirmed."
            )
        }

    override suspend fun queryPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        PaymentResult(success = false, code = "CASH_QUERY_UNSUPPORTED", message = "Cash payments do not support provider query.")
    }

    override suspend fun cancelPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        PaymentResult(success = false, code = "CASH_CANCEL_UNSUPPORTED", message = "Cash payments do not support provider cancel.")
    }
}
