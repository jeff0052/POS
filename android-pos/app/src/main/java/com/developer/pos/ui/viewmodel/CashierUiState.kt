package com.developer.pos.ui.viewmodel

import com.developer.pos.domain.model.CartItem
import com.developer.pos.domain.model.Product

data class CashierUiState(
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val selectedPaymentMethod: String = "SDK_PAY",
    val currentOrderNo: String = "POS-DEMO-0001"
) {
    val filteredProducts: List<Product>
        get() = if (searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.barcode?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

    val totalItems: Int
        get() = cartItems.sumOf { it.quantity }

    val payableAmountCents: Long
        get() = cartItems.sumOf { it.lineAmountCents }
}
