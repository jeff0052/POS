package com.developer.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.pos.data.repository.ProductRepository
import com.developer.pos.domain.model.CartItem
import com.developer.pos.domain.model.Product
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
    private val productRepository: ProductRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val selectedPaymentMethod = MutableStateFlow("SDK_PAY")
    private val currentOrderNo = MutableStateFlow("POS-DEMO-0001")

    val uiState: StateFlow<CashierUiState> = combine(
        productRepository.observeProducts(),
        cartItems,
        searchQuery,
        selectedPaymentMethod,
        currentOrderNo
    ) { products, cart, query, paymentMethod, orderNo ->
        CashierUiState(
            products = products,
            cartItems = cart,
            searchQuery = query,
            selectedPaymentMethod = paymentMethod,
            currentOrderNo = orderNo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CashierUiState()
    )

    init {
        viewModelScope.launch {
            productRepository.seedIfEmpty()
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
    }

    fun increaseQuantity(productId: Long) {
        cartItems.value = cartItems.value.map {
            if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
        }
    }

    fun decreaseQuantity(productId: Long) {
        cartItems.value = cartItems.value.mapNotNull {
            if (it.product.id != productId) return@mapNotNull it
            val nextQuantity = it.quantity - 1
            if (nextQuantity <= 0) null else it.copy(quantity = nextQuantity)
        }
    }

    fun selectPaymentMethod(method: String) {
        selectedPaymentMethod.value = method
    }

    fun prepareMockPayment() {
        currentOrderNo.value = "POS-${System.currentTimeMillis()}"
    }

    fun completeMockPayment() {
        cartItems.value = emptyList()
        currentOrderNo.value = "POS-${System.currentTimeMillis()}"
    }
}
