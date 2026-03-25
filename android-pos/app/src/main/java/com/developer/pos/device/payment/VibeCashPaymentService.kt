package com.developer.pos.device.payment

import com.developer.pos.data.remote.PosOrderApi
import com.developer.pos.data.remote.dto.StartVibeCashPaymentRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STORE_ID = 101L
private const val TABLE_ID = 10002L

@Singleton
class VibeCashPaymentService @Inject constructor(
    private val posOrderApi: PosOrderApi
) : PaymentService {
    private var lastStatus = PaymentConnectionStatus(
        available = true,
        connected = true,
        providerName = "VibeCash",
        message = "VibeCash gateway ready via POS backend"
    )
    private val paymentAttemptIdsByOrderNo = linkedMapOf<String, String>()

    override suspend fun connect(paymentMethod: String): PaymentConnectionStatus = withContext(Dispatchers.IO) {
        val status = when {
            !PaymentMethods.isQr(paymentMethod) -> PaymentConnectionStatus(
                available = false,
                connected = false,
                providerName = "VibeCash",
                message = "VibeCash only supports QR payments."
            )

            else -> PaymentConnectionStatus(
                available = true,
                connected = true,
                providerName = "VibeCash",
                message = "VibeCash gateway ready via POS backend"
            )
        }
        lastStatus = status
        status
    }

    override fun currentStatus(paymentMethod: String): PaymentConnectionStatus {
        return if (PaymentMethods.isQr(paymentMethod)) lastStatus else PaymentConnectionStatus(
            available = false,
            connected = false,
            providerName = "VibeCash",
            message = "VibeCash only supports QR payments."
        )
    }

    override suspend fun startPayment(orderNo: String, amountCents: Long, paymentMethod: String): PaymentResult =
        withContext(Dispatchers.IO) {
            val status = connect(paymentMethod)
            if (!status.available || !status.connected) {
                return@withContext PaymentResult(
                    success = false,
                    code = "VIBECASH_NOT_READY",
                    message = status.message
                )
            }

            runCatching {
                val response = posOrderApi.startVibeCashPayment(
                    storeId = STORE_ID,
                    tableId = TABLE_ID,
                    request = StartVibeCashPaymentRequestDto(toScheme(paymentMethod))
                ).data
                paymentAttemptIdsByOrderNo[orderNo] = response.paymentAttemptId
                PaymentResult(
                    success = false,
                    pending = true,
                    code = response.attemptStatus,
                    message = "VibeCash payment link created. Ask the customer to complete QR payment.",
                    sdkTradeNo = response.providerPaymentId ?: response.paymentAttemptId,
                    actionUrl = response.checkoutUrl
                )
            }.getOrElse { error ->
                PaymentResult(
                    success = false,
                    code = "VIBECASH_REQUEST_FAILED",
                    message = error.message ?: "Failed to create VibeCash payment link via backend."
                )
            }
        }

    override suspend fun queryPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        val paymentAttemptId = paymentAttemptIdsByOrderNo[orderNo]
            ?: return@withContext PaymentResult(
                success = false,
                code = "VIBECASH_NO_ATTEMPT",
                message = "No VibeCash payment attempt found for this order."
            )

        runCatching {
            val attempt = posOrderApi.getVibeCashPaymentAttempt(
                storeId = STORE_ID,
                tableId = TABLE_ID,
                paymentAttemptId = paymentAttemptId
            ).data
            when (attempt.attemptStatus) {
                "SETTLED", "SUCCEEDED" -> PaymentResult(
                    success = true,
                    code = attempt.attemptStatus,
                    message = "VibeCash payment succeeded.",
                    sdkTradeNo = attempt.providerPaymentId
                )

                "FAILED", "EXPIRED" -> PaymentResult(
                    success = false,
                    code = attempt.attemptStatus,
                    message = "VibeCash payment ${attempt.attemptStatus.lowercase()}."
                )

                else -> PaymentResult(
                    success = false,
                    pending = true,
                    code = attempt.attemptStatus,
                    message = "Customer payment is still pending.",
                    sdkTradeNo = attempt.providerPaymentId,
                    actionUrl = attempt.checkoutUrl
                )
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "VIBECASH_QUERY_FAILED",
                message = error.message ?: "Failed to query VibeCash payment status."
            )
        }
    }

    override suspend fun cancelPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        PaymentResult(
            success = false,
            code = "VIBECASH_CANCEL_NOT_IMPLEMENTED",
            message = "VibeCash cancel is not wired yet."
        )
    }

    private fun toScheme(paymentMethod: String): String = when (paymentMethod) {
        PaymentMethods.WECHAT_QR -> "WECHAT_QR"
        PaymentMethods.ALIPAY_QR -> "ALIPAY_QR"
        PaymentMethods.PAYNOW_QR -> "PAYNOW_QR"
        else -> "WECHAT_QR"
    }
}
