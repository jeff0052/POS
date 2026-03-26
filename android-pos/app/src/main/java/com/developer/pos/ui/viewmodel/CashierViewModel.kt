package com.developer.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.pos.data.repository.PosOrderRepository
import com.developer.pos.data.repository.ProductRepository
import com.developer.pos.device.payment.PaymentMethods
import com.developer.pos.device.payment.PaymentService
import com.developer.pos.domain.model.CartItem
import com.developer.pos.domain.model.Product
import com.developer.pos.ui.model.ActiveOrderStage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CashierViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val posOrderRepository: PosOrderRepository,
    private val paymentService: PaymentService
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val selectedPaymentMethod = MutableStateFlow(PaymentMethods.CARD_TERMINAL)
    private val currentOrderNo = MutableStateFlow("POS-DEMO-0001")
    private val activeOrderStage = MutableStateFlow(ActiveOrderStage.DRAFT)
    private val currentActiveOrderId = MutableStateFlow<String?>(null)
    private val syncStatus = MutableStateFlow("Syncing V2 menu")
    private val paymentProviderStatus = MutableStateFlow("Checking DCS SDK")
    private val paymentProcessing = MutableStateFlow(false)
    private val paymentErrorMessage = MutableStateFlow<String?>(null)
    private val paymentActionUrl = MutableStateFlow<String?>(null)
    private val paymentRequiresCustomerAction = MutableStateFlow(false)

    val uiState: StateFlow<CashierUiState> = combine(
        productRepository.observeProducts(),
        cartItems,
        searchQuery,
        selectedPaymentMethod,
        currentOrderNo,
        activeOrderStage,
        syncStatus,
        paymentProviderStatus,
        paymentProcessing,
        paymentErrorMessage,
        paymentActionUrl,
        paymentRequiresCustomerAction
    ) { values ->
        val products = values[0] as List<Product>
        val cart = values[1] as List<CartItem>
        val query = values[2] as String
        val paymentMethod = values[3] as String
        val orderNo = values[4] as String
        val orderStage = values[5] as ActiveOrderStage
        val sync = values[6] as String
        val providerStatus = values[7] as String
        val isProcessing = values[8] as Boolean
        val paymentError = values[9] as String?
        val actionUrl = values[10] as String?
        val requiresCustomerAction = values[11] as Boolean
        CashierUiState(
            products = products,
            cartItems = cart,
            searchQuery = query,
            selectedPaymentMethod = paymentMethod,
            currentOrderNo = orderNo,
            activeOrderStage = orderStage,
            syncStatus = sync,
            paymentProviderStatus = providerStatus,
            paymentProcessing = isProcessing,
            paymentErrorMessage = paymentError,
            paymentActionUrl = actionUrl,
            paymentRequiresCustomerAction = requiresCustomerAction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CashierUiState()
    )

    init {
        viewModelScope.launch {
            productRepository.seedIfEmpty()
            syncStatus.value = "V2 menu ready"
        }
        refreshPaymentProviderStatus()
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addProduct(product: Product) {
        val current = cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current += CartItem(product = product, quantity = 1)
        }
        cartItems.value = current
        activeOrderStage.value = ActiveOrderStage.DRAFT
        syncDraft()
    }

    fun increaseQuantity(productId: Long) {
        cartItems.value = cartItems.value.map {
            if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
        }
        activeOrderStage.value = ActiveOrderStage.DRAFT
        syncDraft()
    }

    fun decreaseQuantity(productId: Long) {
        cartItems.value = cartItems.value.mapNotNull {
            if (it.product.id != productId) return@mapNotNull it
            val nextQuantity = it.quantity - 1
            if (nextQuantity <= 0) null else it.copy(quantity = nextQuantity)
        }
        activeOrderStage.value = ActiveOrderStage.DRAFT
        syncDraft()
    }

    fun selectPaymentMethod(method: String) {
        selectedPaymentMethod.value = method
        paymentErrorMessage.value = null
        paymentActionUrl.value = null
        paymentRequiresCustomerAction.value = false
    }

    fun sendToKitchen() {
        if (cartItems.value.isNotEmpty()) {
            viewModelScope.launch {
                runCatching {
                    ensureDraftSync()
                    val nextStage = currentActiveOrderId.value?.let { posOrderRepository.sendToKitchen(it) }
                        ?: ActiveOrderStage.SUBMITTED
                    activeOrderStage.value = nextStage
                    syncStatus.value = "Sent to kitchen via V2"
                }.onFailure { error ->
                    syncStatus.value = "Send failed: ${error.message ?: "unknown"}"
                }
            }
        }
    }

    fun moveToPendingSettlement() {
        viewModelScope.launch {
            runCatching {
                val scenario = posOrderRepository.moveToPaymentPending()
                com.developer.pos.ui.model.PaymentScenarioStore.current = scenario
                activeOrderStage.value = ActiveOrderStage.PENDING_SETTLEMENT
                syncStatus.value = "Payment pending via V2"
            }.onFailure { error ->
                syncStatus.value = "Payment pending failed: ${error.message ?: "unknown"}"
            }
        }
    }

    fun preparePayment() {
        ensureActiveOrderNo()
        if (activeOrderStage.value != ActiveOrderStage.SETTLED) {
            activeOrderStage.value = ActiveOrderStage.PENDING_SETTLEMENT
        }
        paymentErrorMessage.value = null
        paymentActionUrl.value = null
        paymentRequiresCustomerAction.value = false
    }

    fun startSelectedPayment(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            paymentProcessing.value = true
            paymentErrorMessage.value = null
            paymentActionUrl.value = null
            paymentRequiresCustomerAction.value = false
            runCatching {
                val paymentMethod = selectedPaymentMethod.value
                val connectStatus = paymentService.connect(paymentMethod)
                paymentProviderStatus.value = connectStatus.message
                if (!connectStatus.available || !connectStatus.connected) {
                    error(connectStatus.message)
                }

                val paymentResult = paymentService.startPayment(
                    orderNo = currentOrderNo.value,
                    amountCents = uiState.value.payableAmountCents,
                    paymentMethod = paymentMethod
                )

                if (paymentResult.pending) {
                    paymentProcessing.value = false
                    paymentRequiresCustomerAction.value = true
                    paymentActionUrl.value = paymentResult.actionUrl
                    paymentProviderStatus.value = paymentResult.message ?: "Customer payment action required."
                    paymentErrorMessage.value = paymentResult.actionUrl
                    syncStatus.value = "Customer action pending"
                    onFailure()
                    return@launch
                }

                if (!paymentResult.success) {
                    val failureMessage = buildString {
                        append(paymentResult.code ?: "PAYMENT_PROVIDER_FAILED")
                        paymentResult.message?.takeIf { it.isNotBlank() }?.let {
                            append(": ")
                            append(it)
                        }
                    }
                    paymentProviderStatus.value = failureMessage
                    error(failureMessage)
                }

                val stage = posOrderRepository.collectPayment(
                    paymentMethod = paymentMethod,
                    collectedAmountCents = uiState.value.payableAmountCents
                )
                activeOrderStage.value = stage
                syncStatus.value = "Payment collected via V2"
                paymentProcessing.value = false
                onSuccess()
            }.onFailure { error ->
                paymentProcessing.value = false
                paymentErrorMessage.value = error.message ?: "unknown"
                syncStatus.value = "Collect failed: ${error.message ?: "unknown"}"
                onFailure()
            }
        }
    }

    fun clearPaymentFailure() {
        paymentErrorMessage.value = null
        paymentActionUrl.value = null
        paymentRequiresCustomerAction.value = false
    }

    fun checkSelectedPaymentStatus(
        onSettled: () -> Unit,
        onStillPending: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            val paymentMethod = selectedPaymentMethod.value
            runCatching {
                val result = paymentService.queryPayment(currentOrderNo.value, paymentMethod)
                when {
                    result.success -> {
                        paymentRequiresCustomerAction.value = false
                        paymentActionUrl.value = null
                        paymentErrorMessage.value = null
                        paymentProviderStatus.value = result.message ?: "Payment confirmed."
                        activeOrderStage.value = ActiveOrderStage.SETTLED
                        syncStatus.value = "Payment confirmed via ${PaymentMethods.providerName(paymentMethod)}"
                        onSettled()
                    }

                    result.pending -> {
                        paymentRequiresCustomerAction.value = true
                        paymentActionUrl.value = result.actionUrl
                        paymentErrorMessage.value = result.actionUrl
                        paymentProviderStatus.value = result.message ?: "Customer payment still pending."
                        syncStatus.value = "Customer action pending"
                        onStillPending()
                    }

                    else -> {
                        paymentErrorMessage.value = result.message
                        paymentProviderStatus.value = result.message ?: "Payment query failed."
                        syncStatus.value = "Payment query failed"
                        onFailure()
                    }
                }
            }.onFailure { error ->
                paymentErrorMessage.value = error.message ?: "Payment query failed"
                paymentProviderStatus.value = paymentErrorMessage.value ?: "Payment query failed"
                syncStatus.value = "Payment query failed"
                onFailure()
            }
        }
    }

    fun resetForNextOrder() {
        cartItems.value = emptyList()
        currentOrderNo.value = "POS-${System.currentTimeMillis()}"
        activeOrderStage.value = ActiveOrderStage.DRAFT
        currentActiveOrderId.value = null
        syncStatus.value = "Ready for next V2 order"
        paymentProcessing.value = false
        paymentErrorMessage.value = null
        paymentActionUrl.value = null
        paymentRequiresCustomerAction.value = false
    }

    fun refreshPaymentProviderStatus() {
        viewModelScope.launch {
            val status = paymentService.connect(selectedPaymentMethod.value)
            paymentProviderStatus.value = status.message
        }
    }

    private fun ensureActiveOrderNo() {
        if (currentOrderNo.value == "POS-DEMO-0001") {
            currentOrderNo.value = "POS-${System.currentTimeMillis()}"
        }
    }

    private fun syncDraft() {
        if (cartItems.value.isEmpty()) {
            syncStatus.value = "Draft cleared locally"
            return
        }

        viewModelScope.launch {
            runCatching {
                val draft = posOrderRepository.syncDraft(cartItems.value)
                currentActiveOrderId.value = draft.activeOrderId
                currentOrderNo.value = draft.orderNo
                activeOrderStage.value = draft.stage
                syncStatus.value = "Draft synced to V2"
            }.onFailure { error ->
                syncStatus.value = "Draft sync failed: ${error.message ?: "unknown"}"
            }
        }
    }

    private suspend fun ensureDraftSync() {
        if (cartItems.value.isEmpty()) return
        if (currentActiveOrderId.value != null) return
        val draft = posOrderRepository.syncDraft(cartItems.value)
        currentActiveOrderId.value = draft.activeOrderId
        currentOrderNo.value = draft.orderNo
        activeOrderStage.value = draft.stage
    }
}
