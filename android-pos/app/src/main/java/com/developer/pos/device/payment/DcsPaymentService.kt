package com.developer.pos.device.payment

import android.content.Context
import com.developer.pos.BuildConfig
import com.google.gson.Gson
import com.sunmi.dcspayment.CardTransactionListener
import com.sunmi.dcspayment.CheckCardListener
import com.sunmi.dcspayment.DCSCapabilityProvider
import com.sunmi.dcspayment.DCSCardProvider
import com.sunmi.dcspayment.DCSConfigProvider
import com.sunmi.dcspayment.DCSPaymentProvider
import com.sunmi.dcspayment.DCSQueryTransactionProvider
import com.sunmi.dcspayment.InitListener
import com.sunmi.dcspayment.TransactionListener
import com.sunmi.dcspayment.constant.bean.TransactionResult
import com.sunmi.dcspayment.lib.DCSPaymentApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class DcsTransactionLookup(
    val customerOrderId: String?,
    val referenceNo: String?,
    val traceNo: String?,
    val batchNo: String?,
    val stan: String?,
    val amount: Long,
    val tips: Long,
    val answerCode: String?,
    val answerDesc: String?,
    val transactionType: Int,
    val isVoid: Boolean,
    val isRefund: Boolean,
    val isComplete: Boolean,
    val raw: String
)

@Singleton
class DcsPaymentService @Inject constructor(
    private val appContext: Context
) : PaymentService {
    private val gson = Gson()

    private var api: DCSPaymentApi? = null
    private var capabilityProvider: DCSCapabilityProvider? = null
    private var configProvider: DCSConfigProvider? = null
    private var paymentProvider: DCSPaymentProvider? = null
    private var cardProvider: DCSCardProvider? = null
    private var queryTransactionProvider: DCSQueryTransactionProvider? = null

    private var lastStatus = PaymentConnectionStatus(
        available = false,
        connected = false,
        providerName = "DCS",
        message = "DCS SDK not connected"
    )

    override suspend fun connect(paymentMethod: String): PaymentConnectionStatus = withContext(Dispatchers.IO) {
        if (!PaymentMethods.isCard(paymentMethod)) {
            return@withContext PaymentConnectionStatus(
                available = false,
                connected = false,
                providerName = "DCS",
                message = "DCS only supports card terminal payments."
            )
        }
        if (lastStatus.connected && paymentProvider != null && cardProvider != null) {
            return@withContext lastStatus
        }

        val status = runCatching {
            withTimeout(CONNECT_TIMEOUT_MS) {
                connectInternal()
            }
        }.getOrElse { error ->
            clearSdkState()
            PaymentConnectionStatus(
                available = true,
                connected = false,
                providerName = "DCS",
                message = error.message ?: "Failed to connect DCS SDK"
            )
        }

        lastStatus = status
        status
    }

    override fun currentStatus(paymentMethod: String): PaymentConnectionStatus {
        return if (PaymentMethods.isCard(paymentMethod)) {
            lastStatus
        } else {
            PaymentConnectionStatus(
                available = false,
                connected = false,
                providerName = "DCS",
                message = "DCS only supports card terminal payments."
            )
        }
    }

    override suspend fun startPayment(orderNo: String, amountCents: Long, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        if (!PaymentMethods.isCard(paymentMethod)) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_UNSUPPORTED_METHOD",
                message = "DCS only supports card terminal payments."
            )
        }

        val status = connect(paymentMethod)
        if (!status.available) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_SDK_UNAVAILABLE",
                message = status.message
            )
        }
        if (!status.connected) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_NOT_CONNECTED",
                message = status.message
            )
        }
        if (!hasMerchantConfig()) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_CONFIG_MISSING",
                message = "DCS merchant config is missing. Fill BuildConfig DCS_MERCHANT_ID and DCS_TERMINAL_ID before card payments."
            )
        }

        val merchantConfigCode = runCatching {
            configProvider?.setMerchantParams(buildMerchantParamsJson())
        }.getOrNull()

        if (merchantConfigCode == null) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_CONFIG_PROVIDER_UNAVAILABLE",
                message = "DCS config provider is unavailable."
            )
        }

        if (merchantConfigCode != 0) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_CONFIG_REJECTED",
                message = "DCS merchant params were rejected with code $merchantConfigCode."
            )
        }

        val signInResult = signIn()
        if (!signInResult.success) {
            return@withContext signInResult
        }

        runCatching {
            withTimeout(PAYMENT_TIMEOUT_MS) {
                startCardSaleInternal(orderNo, amountCents)
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_PAYMENT_TIMEOUT",
                message = error.message ?: "DCS payment timed out."
            )
        }
    }

    override suspend fun queryPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        if (!PaymentMethods.isCard(paymentMethod)) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_UNSUPPORTED_METHOD",
                message = "DCS only supports card terminal payments."
            )
        }

        val status = connect(paymentMethod)
        if (!status.available || !status.connected) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_NOT_CONNECTED",
                message = status.message
            )
        }

        val queryProvider = queryTransactionProvider
            ?: return@withContext PaymentResult(
                success = false,
                code = "DCS_QUERY_PROVIDER_UNAVAILABLE",
                message = "DCS query provider is unavailable."
            )

        runCatching {
            val raw = queryProvider.queryTransByOrderId(orderNo)
            parseQueryResult(raw, orderNo)
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_QUERY_FAILED",
                message = error.message ?: "Failed to query DCS payment."
            )
        }
    }

    override suspend fun cancelPayment(orderNo: String, paymentMethod: String): PaymentResult = withContext(Dispatchers.IO) {
        if (!PaymentMethods.isCard(paymentMethod)) {
            return@withContext PaymentResult(
                success = false,
                code = "DCS_UNSUPPORTED_METHOD",
                message = "DCS only supports card terminal payments."
            )
        }

        voidSale(
            originalOrderNo = orderNo,
            voidOrderNo = "${orderNo.take(24)}-VOID-${System.currentTimeMillis().toString().takeLast(6)}"
        )
    }

    suspend fun queryTransactionDetail(orderNo: String): DcsTransactionLookup? = withContext(Dispatchers.IO) {
        val status = connect(PaymentMethods.CARD_TERMINAL)
        if (!status.available || !status.connected) {
            return@withContext null
        }

        val raw = runCatching { queryTransactionProvider?.queryTransByOrderId(orderNo) }.getOrNull()
        parseTransactionLookup(raw)
    }

    suspend fun voidSale(originalOrderNo: String, voidOrderNo: String): PaymentResult = withContext(Dispatchers.IO) {
        val status = connect(PaymentMethods.CARD_TERMINAL)
        if (!status.available || !status.connected) {
            return@withContext PaymentResult(false, code = "DCS_NOT_CONNECTED", message = status.message)
        }

        val original = queryTransactionDetail(originalOrderNo)
            ?: return@withContext PaymentResult(
                success = false,
                code = "DCS_ORIGINAL_NOT_FOUND",
                message = "Original DCS transaction not found for $originalOrderNo."
            )

        val provider = paymentProvider
        if (provider == null) {
            return@withContext PaymentResult(false, code = "DCS_PROVIDER_UNAVAILABLE", message = "DCS payment provider is unavailable.")
        }

        runCatching {
            withTimeout(PAYMENT_TIMEOUT_MS) {
                executeCheckCardOperation { listener ->
                    provider.saleVoid(
                        voidOrderNo,
                        original.amount,
                        original.tips,
                        original.referenceNo.orEmpty(),
                        original.traceNo ?: original.stan.orEmpty(),
                        original.batchNo?.toIntOrNull() ?: 0,
                        listener
                    )
                }
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_VOID_FAILED",
                message = error.message ?: "DCS sale void failed."
            )
        }
    }

    suspend fun refundSale(originalOrderNo: String, refundOrderNo: String, amountCents: Long): PaymentResult = withContext(Dispatchers.IO) {
        val status = connect(PaymentMethods.CARD_TERMINAL)
        if (!status.available || !status.connected) {
            return@withContext PaymentResult(false, code = "DCS_NOT_CONNECTED", message = status.message)
        }

        val original = queryTransactionDetail(originalOrderNo)
            ?: return@withContext PaymentResult(
                success = false,
                code = "DCS_ORIGINAL_NOT_FOUND",
                message = "Original DCS transaction not found for $originalOrderNo."
            )

        val provider = paymentProvider
        if (provider == null) {
            return@withContext PaymentResult(false, code = "DCS_PROVIDER_UNAVAILABLE", message = "DCS payment provider is unavailable.")
        }

        runCatching {
            withTimeout(PAYMENT_TIMEOUT_MS) {
                executeCheckCardOperation { listener ->
                    provider.cardRefund(
                        refundOrderNo,
                        amountCents,
                        original.referenceNo.orEmpty(),
                        original.traceNo ?: original.stan.orEmpty(),
                        listener
                    )
                }
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_REFUND_FAILED",
                message = error.message ?: "DCS card refund failed."
            )
        }
    }

    suspend fun terminalSettlement(): PaymentResult = withContext(Dispatchers.IO) {
        val status = connect(PaymentMethods.CARD_TERMINAL)
        if (!status.available || !status.connected) {
            return@withContext PaymentResult(false, code = "DCS_NOT_CONNECTED", message = status.message)
        }

        val provider = paymentProvider
            ?: return@withContext PaymentResult(false, code = "DCS_PROVIDER_UNAVAILABLE", message = "DCS payment provider is unavailable.")

        runCatching {
            suspendCancellableCoroutine { continuation ->
                provider.settlment(object : TransactionListener.Stub() {
                    override fun onSuccess() {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(PaymentResult(success = true, message = "DCS terminal settlement completed."))
                        }
                    }

                    override fun onFailed(code: String?, message: String?) {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(
                                PaymentResult(
                                    success = false,
                                    code = code ?: "DCS_SETTLEMENT_FAILED",
                                    message = message ?: "DCS terminal settlement failed."
                                )
                            )
                        }
                    }

                    override fun transactionResult(result: String?) {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(
                                PaymentResult(
                                    success = true,
                                    message = result ?: "DCS terminal settlement completed."
                                )
                            )
                        }
                    }

                    override fun startRequest() = Unit
                })
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_SETTLEMENT_FAILED",
                message = error.message ?: "DCS terminal settlement failed."
            )
        }
    }

    suspend fun signOffTerminal(): PaymentResult = withContext(Dispatchers.IO) {
        val status = connect(PaymentMethods.CARD_TERMINAL)
        if (!status.available || !status.connected) {
            return@withContext PaymentResult(false, code = "DCS_NOT_CONNECTED", message = status.message)
        }

        val provider = paymentProvider
            ?: return@withContext PaymentResult(false, code = "DCS_PROVIDER_UNAVAILABLE", message = "DCS payment provider is unavailable.")

        runCatching {
            suspendCancellableCoroutine { continuation ->
                provider.signOff(object : TransactionListener.Stub() {
                    override fun onSuccess() {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(PaymentResult(success = true, message = "DCS terminal sign off completed."))
                        }
                    }

                    override fun onFailed(code: String?, message: String?) {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(
                                PaymentResult(
                                    success = false,
                                    code = code ?: "DCS_SIGNOFF_FAILED",
                                    message = message ?: "DCS terminal sign off failed."
                                )
                            )
                        }
                    }

                    override fun transactionResult(result: String?) {
                        if (continuation.context.isActiveLike()) {
                            continuation.resume(
                                PaymentResult(
                                    success = true,
                                    message = result ?: "DCS terminal sign off completed."
                                )
                            )
                        }
                    }

                    override fun startRequest() = Unit
                })
            }
        }.getOrElse { error ->
            PaymentResult(
                success = false,
                code = "DCS_SIGNOFF_FAILED",
                message = error.message ?: "DCS terminal sign off failed."
            )
        }
    }

    private suspend fun connectInternal(): PaymentConnectionStatus = suspendCancellableCoroutine { continuation ->
        val sdk = DCSPaymentApi.getInstance()
        api = sdk
        sdk.connectDCSPayment(appContext, object : DCSPaymentApi.ConnCallback {
            override fun onServiceConnected() {
                capabilityProvider = sdk.getDCSCapabilityProvider()
                configProvider = sdk.getDCSConfigProvider()
                paymentProvider = sdk.getDCSPaymentProvider()
                cardProvider = sdk.getDCSCardProvider()
                queryTransactionProvider = sdk.getDCSQueryTransactionProvider()

                val capability = capabilityProvider
                if (capability == null || configProvider == null || paymentProvider == null || cardProvider == null || queryTransactionProvider == null) {
                    clearSdkState()
                    if (continuation.isActive) {
                        continuation.resume(
                            PaymentConnectionStatus(
                                available = true,
                                connected = false,
                                providerName = "DCS",
                                message = "DCS service connected but required providers are missing."
                            )
                        )
                    }
                    return
                }

                capability.init(
                    BuildConfig.DCS_COUNTRY_CODE,
                    BuildConfig.DCS_CURRENCY_CODE,
                    object : InitListener.Stub() {
                        override fun onSuccess() {
                            if (continuation.isActive) {
                                continuation.resume(
                                    PaymentConnectionStatus(
                                        available = true,
                                        connected = true,
                                        providerName = "DCS",
                                        message = if (hasMerchantConfig()) {
                                            "DCS connected and initialized"
                                        } else {
                                            "DCS connected. Merchant config still needs to be filled."
                                        }
                                    )
                                )
                            }
                        }

                        override fun onFailed(code: String?, message: String?) {
                            clearSdkState()
                            if (continuation.isActive) {
                                continuation.resume(
                                    PaymentConnectionStatus(
                                        available = true,
                                        connected = false,
                                        providerName = "DCS",
                                        message = message ?: "DCS init failed ($code)"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            override fun onServiceDisconnected() {
                clearSdkState()
                lastStatus = PaymentConnectionStatus(
                    available = true,
                    connected = false,
                    providerName = "DCS",
                    message = "DCS service disconnected"
                )
                if (continuation.isActive) {
                    continuation.resume(lastStatus)
                }
            }

            override fun onNotFoundService() {
                clearSdkState()
                if (continuation.isActive) {
                    continuation.resume(
                        PaymentConnectionStatus(
                            available = false,
                            connected = false,
                            providerName = "DCS",
                            message = "DCS payment service not found on this device."
                        )
                    )
                }
            }
        })
    }

    private suspend fun signIn(): PaymentResult = suspendCancellableCoroutine { continuation ->
        val provider = paymentProvider
        if (provider == null) {
            continuation.resume(
                PaymentResult(
                    success = false,
                    code = "DCS_PROVIDER_UNAVAILABLE",
                    message = "DCS payment provider is unavailable."
                )
            )
            return@suspendCancellableCoroutine
        }

        provider.sign(object : TransactionListener.Stub() {
            override fun onSuccess() {
                if (continuation.isActive) {
                    continuation.resume(PaymentResult(success = true))
                }
            }

            override fun onFailed(code: String?, message: String?) {
                if (continuation.isActive) {
                    continuation.resume(
                        PaymentResult(
                            success = false,
                            code = code ?: "DCS_SIGN_FAILED",
                            message = message ?: "DCS sign failed."
                        )
                    )
                }
            }

            override fun transactionResult(result: String?) {
                if (continuation.isActive) {
                    continuation.resume(
                        PaymentResult(
                            success = true,
                            message = result
                        )
                    )
                }
            }

            override fun startRequest() = Unit
        })
    }

    private suspend fun startCardSaleInternal(orderNo: String, amountCents: Long): PaymentResult =
        suspendCancellableCoroutine { continuation ->
            val provider = paymentProvider
            val card = cardProvider
            if (provider == null || card == null) {
                continuation.resume(
                    PaymentResult(
                        success = false,
                        code = "DCS_PROVIDER_UNAVAILABLE",
                        message = "DCS providers are not ready."
                    )
                )
                return@suspendCancellableCoroutine
            }

            val cardFlowListener = object : CardTransactionListener.Stub() {
                override fun onWaitAppSelect(apps: MutableList<String>?, firstSelect: Boolean) {
                    runCatching { card.confirmAppSelect(0) }
                        .onFailure { finishWithFailure(continuation, "DCS_APP_SELECT_FAILED", it.message) }
                }

                override fun onAppFinalSelect(appName: String?) {
                    runCatching { card.confirmAppFinalSelect(true) }
                        .onFailure { finishWithFailure(continuation, "DCS_APP_FINAL_FAILED", it.message) }
                }

                override fun onConfirmCardNo(cardNo: String?) {
                    runCatching { card.confirmCardNo(true) }
                        .onFailure { finishWithFailure(continuation, "DCS_CARD_CONFIRM_FAILED", it.message) }
                }

                override fun onCardHolderCertVerify(certType: String?, certNo: String?) {
                    runCatching { card.confirmCertVerify(true) }
                        .onFailure { finishWithFailure(continuation, "DCS_CERT_VERIFY_FAILED", it.message) }
                }

                override fun onError(code: String?, message: String?) {
                    finishWithFailure(continuation, code ?: "DCS_CARD_FLOW_ERROR", message)
                }

                override fun transactionResult(result: String?) {
                    finishWithTransactionResult(continuation, result)
                }

                override fun startRequest() = Unit
            }

            provider.cardSaleTransaction(
                orderNo,
                amountCents,
                0L,
                object : CheckCardListener.Stub() {
                    override fun onFindMagCard(track2Data: String?) {
                        runCatching { card.confirmCheckCard(cardFlowListener) }
                            .onFailure { finishWithFailure(continuation, "DCS_MAG_CARD_FAILED", it.message) }
                    }

                    override fun onFindContactlessCard() {
                        runCatching { card.confirmCheckCard(cardFlowListener) }
                            .onFailure { finishWithFailure(continuation, "DCS_CONTACTLESS_FAILED", it.message) }
                    }

                    override fun onFindContactCard() {
                        runCatching { card.confirmCheckCard(cardFlowListener) }
                            .onFailure { finishWithFailure(continuation, "DCS_CONTACT_CARD_FAILED", it.message) }
                    }

                    override fun onError(code: String?, message: String?) {
                        finishWithFailure(continuation, code ?: "DCS_CHECK_CARD_ERROR", message)
                    }

                    override fun transactionResult(result: String?) {
                        finishWithTransactionResult(continuation, result)
                    }

                    override fun startRequest() = Unit
                }
            )
        }

    private suspend fun executeCheckCardOperation(
        action: (CheckCardListener.Stub) -> Unit
    ): PaymentResult = suspendCancellableCoroutine { continuation ->
        val card = cardProvider
        if (card == null) {
            continuation.resume(
                PaymentResult(
                    success = false,
                    code = "DCS_CARD_PROVIDER_UNAVAILABLE",
                    message = "DCS card provider is unavailable."
                )
            )
            return@suspendCancellableCoroutine
        }

        val cardFlowListener = object : CardTransactionListener.Stub() {
            override fun onWaitAppSelect(apps: MutableList<String>?, firstSelect: Boolean) {
                runCatching { card.confirmAppSelect(0) }
                    .onFailure { finishWithFailure(continuation, "DCS_APP_SELECT_FAILED", it.message) }
            }

            override fun onAppFinalSelect(appName: String?) {
                runCatching { card.confirmAppFinalSelect(true) }
                    .onFailure { finishWithFailure(continuation, "DCS_APP_FINAL_FAILED", it.message) }
            }

            override fun onConfirmCardNo(cardNo: String?) {
                runCatching { card.confirmCardNo(true) }
                    .onFailure { finishWithFailure(continuation, "DCS_CARD_CONFIRM_FAILED", it.message) }
            }

            override fun onCardHolderCertVerify(certType: String?, certNo: String?) {
                runCatching { card.confirmCertVerify(true) }
                    .onFailure { finishWithFailure(continuation, "DCS_CERT_VERIFY_FAILED", it.message) }
            }

            override fun onError(code: String?, message: String?) {
                finishWithFailure(continuation, code ?: "DCS_CARD_FLOW_ERROR", message)
            }

            override fun transactionResult(result: String?) {
                finishWithTransactionResult(continuation, result)
            }

            override fun startRequest() = Unit
        }

        val listener = object : CheckCardListener.Stub() {
            override fun onFindMagCard(track2Data: String?) {
                runCatching { card.confirmCheckCard(cardFlowListener) }
                    .onFailure { finishWithFailure(continuation, "DCS_MAG_CARD_FAILED", it.message) }
            }

            override fun onFindContactlessCard() {
                runCatching { card.confirmCheckCard(cardFlowListener) }
                    .onFailure { finishWithFailure(continuation, "DCS_CONTACTLESS_FAILED", it.message) }
            }

            override fun onFindContactCard() {
                runCatching { card.confirmCheckCard(cardFlowListener) }
                    .onFailure { finishWithFailure(continuation, "DCS_CONTACT_CARD_FAILED", it.message) }
            }

            override fun onError(code: String?, message: String?) {
                finishWithFailure(continuation, code ?: "DCS_CHECK_CARD_ERROR", message)
            }

            override fun transactionResult(result: String?) {
                finishWithTransactionResult(continuation, result)
            }

            override fun startRequest() = Unit
        }

        action(listener)
    }

    private fun finishWithTransactionResult(
        continuation: kotlin.coroutines.Continuation<PaymentResult>,
        result: String?
    ) {
        if (!continuation.context.isActiveLike() || result == null) {
            if (continuation.context.isActiveLike()) {
                continuation.resume(
                    PaymentResult(
                        success = false,
                        code = "DCS_EMPTY_RESULT",
                        message = "DCS returned an empty transaction result."
                    )
                )
            }
            return
        }

        val parsed = runCatching { gson.fromJson(result, TransactionResult::class.java) }.getOrNull()
        val answerCode = parsed?.answerCode?.orEmpty().orEmpty()
        val success = answerCode.isBlank() || answerCode == "00" || answerCode == "0000" || answerCode.equals("SUCCESS", ignoreCase = true)
        val sdkTradeNo = parsed?.customerPaymentId
            ?: parsed?.referenceNo
            ?: parsed?.traceNo
            ?: parsed?.stan

        continuation.resume(
            PaymentResult(
                success = success,
                code = if (success) null else answerCode.ifBlank { "DCS_DECLINED" },
                message = parsed?.answerDesc ?: result,
                sdkTradeNo = sdkTradeNo
            )
        )
    }

    private fun finishWithFailure(
        continuation: kotlin.coroutines.Continuation<PaymentResult>,
        code: String,
        message: String?
    ) {
        if (continuation.context.isActiveLike()) {
            continuation.resume(
                PaymentResult(
                    success = false,
                    code = code,
                    message = message ?: code
                )
            )
        }
    }

    private fun buildMerchantParamsJson(): String {
        val payload = linkedMapOf(
            "merchantId" to BuildConfig.DCS_MERCHANT_ID,
            "terminalId" to BuildConfig.DCS_TERMINAL_ID
        )
        return gson.toJson(payload)
    }

    private fun hasMerchantConfig(): Boolean {
        return BuildConfig.DCS_MERCHANT_ID.isNotBlank() && BuildConfig.DCS_TERMINAL_ID.isNotBlank()
    }

    private fun clearSdkState() {
        capabilityProvider = null
        configProvider = null
        paymentProvider = null
        cardProvider = null
        queryTransactionProvider = null
    }

    private fun parseQueryResult(raw: String?, orderNo: String): PaymentResult {
        if (raw.isNullOrBlank()) {
            return PaymentResult(
                success = false,
                code = "DCS_QUERY_EMPTY",
                message = "No local DCS transaction found for $orderNo."
            )
        }

        val lowered = raw.lowercase()
        return when {
            lowered.contains("approved") ||
                lowered.contains("success") ||
                lowered.contains("complete") ||
                lowered.contains("paid") -> PaymentResult(
                success = true,
                code = "DCS_QUERY_SUCCESS",
                message = "DCS transaction confirmed.",
                sdkTradeNo = extractReference(raw)
            )

            lowered.contains("pending") ||
                lowered.contains("processing") ||
                lowered.contains("init") -> PaymentResult(
                success = false,
                pending = true,
                code = "DCS_QUERY_PENDING",
                message = "DCS transaction is still pending.",
                sdkTradeNo = extractReference(raw)
            )

            lowered.contains("failed") ||
                lowered.contains("void") ||
                lowered.contains("cancel") ||
                lowered.contains("declined") -> PaymentResult(
                success = false,
                code = "DCS_QUERY_FAILED",
                message = "DCS transaction was not approved.",
                sdkTradeNo = extractReference(raw)
            )

            else -> PaymentResult(
                success = false,
                code = "DCS_QUERY_UNKNOWN",
                message = raw,
                sdkTradeNo = extractReference(raw)
            )
        }
    }

    private fun parseTransactionLookup(raw: String?): DcsTransactionLookup? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val parsed = runCatching { gson.fromJson(raw, TransactionResult::class.java) }.getOrNull() ?: return null
        return DcsTransactionLookup(
            customerOrderId = parsed.customerOrderId,
            referenceNo = parsed.referenceNo,
            traceNo = parsed.traceNo,
            batchNo = parsed.batchNo,
            stan = parsed.stan,
            amount = parsed.amount,
            tips = parsed.tips,
            answerCode = parsed.answerCode,
            answerDesc = parsed.answerDesc,
            transactionType = parsed.transactionType,
            isVoid = parsed.isVoid,
            isRefund = parsed.isRefund,
            isComplete = parsed.isComplete,
            raw = raw
        )
    }

    private fun extractReference(raw: String): String? {
        val patterns = listOf(
            "\"referenceNo\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"rrn\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"voucherNo\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"stan\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.find(raw)?.groupValues?.getOrNull(1)
        }
    }

    private fun kotlin.coroutines.CoroutineContext.isActiveLike(): Boolean = this[kotlinx.coroutines.Job]?.isActive != false

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        const val PAYMENT_TIMEOUT_MS = 45_000L
    }
}
