package com.developer.pos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.developer.pos.ui.model.PaymentScenario
import com.developer.pos.ui.model.PaymentScenarioStore
import com.developer.pos.ui.screens.cashier.CashierScreen
import com.developer.pos.ui.screens.home.HomeScreen
import com.developer.pos.ui.screens.login.LoginScreen
import com.developer.pos.ui.screens.orderdetail.OrderDetailScreen
import com.developer.pos.ui.screens.orders.OrdersScreen
import com.developer.pos.ui.screens.payment.PaymentConfirmScreen
import com.developer.pos.ui.screens.payment.PaymentFailureScreen
import com.developer.pos.ui.screens.payment.PaymentProcessingScreen
import com.developer.pos.ui.screens.payment.PaymentSuccessScreen
import com.developer.pos.ui.screens.payment.RefundResultScreen
import com.developer.pos.ui.screens.payment.RefundScreen
import com.developer.pos.ui.screens.settings.SettingsScreen
import com.developer.pos.ui.screens.settlement.SettlementScreen
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PosApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Login.route
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Home.route) {
            HomeScreen(
                onStartCashier = { navController.navigate(Routes.Cashier.route) },
                onOpenOrders = { navController.navigate(Routes.Orders.route) },
                onOpenSettlement = { navController.navigate(Routes.Settlement.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }
        composable(Routes.Cashier.route) {
            val viewModel: CashierViewModel = hiltViewModel()
            CashierScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onProceedToPayment = {
                    PaymentScenarioStore.current = PaymentScenario.fromPos(viewModel.uiState.value.payableAmountCents)
                    navController.navigate(Routes.PaymentConfirm.route)
                }
            )
        }
        composable(Routes.PaymentConfirm.route) {
            val viewModel: CashierViewModel = hiltViewModel()
            PaymentConfirmScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartPayment = { navController.navigate(Routes.PaymentProcessing.route) }
            )
        }
        composable(Routes.PaymentProcessing.route) {
            val viewModel: CashierViewModel = hiltViewModel()
            PaymentProcessingScreen(
                viewModel = viewModel,
                onPaymentSuccess = {
                    navController.navigate(Routes.PaymentSuccess.route) {
                        popUpTo(Routes.PaymentConfirm.route) { inclusive = true }
                    }
                },
                onPaymentFailure = {
                    navController.navigate(Routes.PaymentFailure.route) {
                        popUpTo(Routes.PaymentConfirm.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PaymentSuccess.route) {
            val viewModel: CashierViewModel = hiltViewModel()
            PaymentSuccessScreen(
                viewModel = viewModel,
                onFinish = {
                    viewModel.completeMockPayment()
                    PaymentScenarioStore.current = PaymentScenario.fromPos(0L)
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PaymentFailure.route) {
            val viewModel: CashierViewModel = hiltViewModel()
            PaymentFailureScreen(
                viewModel = viewModel,
                onRetry = { navController.navigate(Routes.PaymentProcessing.route) },
                onBackToCashier = { navController.popBackStack(Routes.Cashier.route, false) }
            )
        }
        composable(Routes.Orders.route) {
            OrdersScreen(
                onBack = { navController.popBackStack() },
                onOpenOrderDetail = { navController.navigate(Routes.OrderDetail.route) }
            )
        }
        composable(Routes.OrderDetail.route) {
            OrderDetailScreen(
                onBack = { navController.popBackStack() },
                onRefund = { navController.navigate(Routes.Refund.route) },
                onProceedToPayment = {
                    PaymentScenarioStore.current = PaymentScenario.qrDemo()
                    navController.navigate(Routes.PaymentConfirm.route)
                }
            )
        }
        composable(Routes.Refund.route) {
            RefundScreen(
                onBack = { navController.popBackStack() },
                onSubmitRefund = {
                    navController.navigate(Routes.RefundResult.route) {
                        popUpTo(Routes.Refund.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.RefundResult.route) {
            RefundResultScreen(
                onFinish = { navController.popBackStack(Routes.Orders.route, false) }
            )
        }
        composable(Routes.Settlement.route) {
            SettlementScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
