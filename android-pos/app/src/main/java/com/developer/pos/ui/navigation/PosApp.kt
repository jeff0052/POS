package com.developer.pos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.developer.pos.ui.viewmodel.DcsTerminalViewModel
import com.developer.pos.ui.viewmodel.OperationsViewModel

@Composable
fun PosApp() {
    val navController = rememberNavController()
    val cashierViewModel: CashierViewModel = hiltViewModel()
    val operationsViewModel: OperationsViewModel = hiltViewModel()
    val dcsTerminalViewModel: DcsTerminalViewModel = hiltViewModel()
    val operationsUiState by operationsViewModel.uiState.collectAsStateWithLifecycle()
    val dcsTerminalUiState by dcsTerminalViewModel.uiState.collectAsStateWithLifecycle()

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
                uiState = operationsUiState,
                onStartCashier = { navController.navigate(Routes.Cashier.route) },
                onOpenOrders = { navController.navigate(Routes.Orders.route) },
                onOpenSettlement = { navController.navigate(Routes.Settlement.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }
        composable(Routes.Cashier.route) {
            CashierScreen(
                viewModel = cashierViewModel,
                onBack = { navController.popBackStack() },
                onProceedToPayment = {
                    PaymentScenarioStore.current = PaymentScenario.fromPos(cashierViewModel.uiState.value.payableAmountCents)
                    navController.navigate(Routes.PaymentConfirm.route)
                }
            )
        }
        composable(Routes.PaymentConfirm.route) {
            PaymentConfirmScreen(
                viewModel = cashierViewModel,
                onBack = { navController.popBackStack() },
                onStartPayment = { navController.navigate(Routes.PaymentProcessing.route) }
            )
        }
        composable(Routes.PaymentProcessing.route) {
            PaymentProcessingScreen(
                viewModel = cashierViewModel,
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
            PaymentSuccessScreen(
                viewModel = cashierViewModel,
                onFinish = {
                    cashierViewModel.resetForNextOrder()
                    operationsViewModel.refresh()
                    PaymentScenarioStore.current = PaymentScenario.fromPos(0L)
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PaymentFailure.route) {
            PaymentFailureScreen(
                viewModel = cashierViewModel,
                onRetry = {
                    if (cashierViewModel.uiState.value.paymentRequiresCustomerAction) {
                        cashierViewModel.checkSelectedPaymentStatus(
                            onSettled = {
                                operationsViewModel.refresh()
                                navController.navigate(Routes.PaymentSuccess.route) {
                                    popUpTo(Routes.PaymentConfirm.route) { inclusive = true }
                                }
                            },
                            onStillPending = {},
                            onFailure = {}
                        )
                    } else {
                        navController.navigate(Routes.PaymentProcessing.route)
                    }
                },
                onBackToCashier = { navController.popBackStack(Routes.Cashier.route, false) }
            )
        }
        composable(Routes.Orders.route) {
            OrdersScreen(
                uiState = operationsUiState,
                onBack = { navController.popBackStack() },
                onOpenOrderDetail = { order ->
                    operationsViewModel.selectOrder(order)
                    navController.navigate(Routes.OrderDetail.route)
                }
            )
        }
        composable(Routes.OrderDetail.route) {
            OrderDetailScreen(
                order = operationsUiState.selectedOrder,
                onBack = { navController.popBackStack() },
                onRefund = { navController.navigate(Routes.Refund.route) },
                onProceedToPayment = {
                    val order = operationsUiState.selectedOrder
                    if (order != null) {
                        PaymentScenarioStore.current = PaymentScenario(
                            source = order.orderType,
                            tableCode = order.tableCode,
                            memberName = order.memberName,
                            memberTier = order.memberTier,
                            originalAmountCents = order.originalAmountCents,
                            memberDiscountCents = order.memberDiscountCents,
                            promotionDiscountCents = order.promotionDiscountCents,
                            payableAmountCents = order.payableAmountCents,
                            giftItems = order.giftItems,
                            headline = "Active table order ready for cashier settlement"
                        )
                    }
                    navController.navigate(Routes.PaymentConfirm.route)
                }
            )
        }
        composable(Routes.Refund.route) {
            RefundScreen(
                order = operationsUiState.selectedOrder,
                uiState = dcsTerminalUiState,
                onBack = { navController.popBackStack() },
                onVoidSale = {
                    operationsUiState.selectedOrder?.let { order ->
                        dcsTerminalViewModel.voidSale(order) {
                            navController.navigate(Routes.RefundResult.route) {
                                popUpTo(Routes.Refund.route) { inclusive = true }
                            }
                        }
                    }
                },
                onSubmitRefund = {
                    operationsUiState.selectedOrder?.let { order ->
                        dcsTerminalViewModel.refundSale(order) {
                            navController.navigate(Routes.RefundResult.route) {
                                popUpTo(Routes.Refund.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        composable(Routes.RefundResult.route) {
            RefundResultScreen(
                uiState = dcsTerminalUiState,
                onFinish = { navController.popBackStack(Routes.Orders.route, false) }
            )
        }
        composable(Routes.Settlement.route) {
            SettlementScreen(
                uiState = dcsTerminalUiState,
                onBack = { navController.popBackStack() },
                onRefreshStatus = { dcsTerminalViewModel.refreshStatus() },
                onRunSettlement = { dcsTerminalViewModel.runTerminalSettlement() },
                onSignOff = { dcsTerminalViewModel.signOffTerminal() }
            )
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
