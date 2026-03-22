package com.developer.pos.ui.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Home : Routes("home")
    data object Cashier : Routes("cashier")
    data object PaymentConfirm : Routes("payment-confirm")
    data object PaymentProcessing : Routes("payment-processing")
    data object PaymentSuccess : Routes("payment-success")
    data object PaymentFailure : Routes("payment-failure")
    data object Orders : Routes("orders")
    data object OrderDetail : Routes("order-detail")
    data object Refund : Routes("refund")
    data object RefundResult : Routes("refund-result")
    data object Settlement : Routes("settlement")
    data object Settings : Routes("settings")
}
