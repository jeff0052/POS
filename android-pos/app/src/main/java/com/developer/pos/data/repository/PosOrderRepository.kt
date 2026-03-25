package com.developer.pos.data.repository

import com.developer.pos.domain.model.CartItem
import com.developer.pos.ui.model.ActiveOrderStage
import com.developer.pos.ui.model.PaymentScenario

interface PosOrderRepository {
    suspend fun syncDraft(cartItems: List<CartItem>): PosDraftState
    suspend fun sendToKitchen(activeOrderId: String): ActiveOrderStage
    suspend fun moveToPaymentPending(): PaymentScenario
    suspend fun collectPayment(paymentMethod: String, collectedAmountCents: Long): ActiveOrderStage
}

data class PosDraftState(
    val activeOrderId: String,
    val orderNo: String,
    val stage: ActiveOrderStage,
    val payableAmountCents: Long
)
