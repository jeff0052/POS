package com.developer.pos.domain.model

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineAmountCents: Long
        get() = product.priceCents * quantity
}
