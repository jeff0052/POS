package com.developer.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.pos.data.repository.PosOrderRepository
import com.developer.pos.data.repository.ProductRepository
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
    private val posOrderRepository: PosOrderRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val selectedPaymentMethod = MutableStateFlow("SDK_PAY")
    private val currentOrderNo = MutableStateFlow("POS-DEMO-0001")
    private val activeOrderStage = MutableStateFlow(ActiveOrderStage.DRAFT)
    private val currentActiveOrderId = MutableStateFlow<String?>(null)
    private val syncStatus = MutableStateFlow("Syncing V2 menu")

    val uiState: StateFlow<CashierUiState> = combine(
        productRepository.observeProducts(),
        cartItems,
        searchQuery,
        selectedPaymentMethod,
        currentOrderNo,
        activeOrderStage,
        syncStatus
    ) { products, cart, query, paymentMethod, orderNo, orderStage, sync ->
        CashierUiState(
            products = products,
            cartItems = cart,
            searchQuery = query,
            selectedPaymentMethod = paymentMethod,
            currentOrderNo = orderNo,
            activeOrderStage = orderStage,
            syncStatus = sync
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

    fun prepareMockPayment() {
        ensureActiveOrderNo()
        if (activeOrderStage.value != ActiveOrderStage.SETTLED) {
            activeOrderStage.value = ActiveOrderStage.PENDING_SETTLEMENT
        }
    }

    fun completeMockPayment() {
        viewModelScope.launch {
            runCatching {
                val stage = posOrderRepository.collectPayment(
                    paymentMethod = selectedPaymentMethod.value,
                    collectedAmountCents = uiState.value.payableAmountCents
                )
                activeOrderStage.value = stage
                syncStatus.value = "Payment collected via V2"
            }.onFailure { error ->
                syncStatus.value = "Collect failed: ${error.message ?: "unknown"}"
            }
        }
    }

    fun resetForNextOrder() {
        cartItems.value = emptyList()
        currentOrderNo.value = "POS-${System.currentTimeMillis()}"
        activeOrderStage.value = ActiveOrderStage.DRAFT
        currentActiveOrderId.value = null
        syncStatus.value = "Ready for next V2 order"
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
