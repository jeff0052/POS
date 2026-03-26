package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.developer.pos.device.payment.PaymentMethods
import com.developer.pos.ui.model.ActiveOrderStage
import com.developer.pos.ui.model.PaymentScenarioStore
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PaymentConfirmScreen(
    viewModel: CashierViewModel,
    onBack: () -> Unit,
    onStartPayment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario = PaymentScenarioStore.current
    val payableAmountCents = if (scenario.payableAmountCents > 0L) scenario.payableAmountCents else uiState.payableAmountCents
    val currentStage = if (scenario.source == "QR") ActiveOrderStage.PENDING_SETTLEMENT else uiState.activeOrderStage
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (scenario.source == "QR") "Cashier Settlement" else "Order Review and Settlement",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Settlement Context", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(scenario.source) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(currentStage.label) }
                    )
                    scenario.tableCode?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text(it) }
                        )
                    }
                }
                Text(scenario.headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (scenario.source == "QR") {
                        "Customer already submitted this table order. Cashier confirms discounts and collects payment at the register."
                    } else {
                        "Staff-created active table order. Review discounts and move this order into cashier settlement."
                    }
                )
                Text(
                    text = if (scenario.memberTier != null) {
                        "Member: ${scenario.memberName} / ${scenario.memberTier}"
                    } else {
                        "Member: ${scenario.memberName ?: "Guest"}"
                    }
                )
                Text("Original Amount: SGD %.2f".format(scenario.originalAmountCents / 100.0))
                Text("Member Discount: -SGD %.2f".format(scenario.memberDiscountCents / 100.0))
                Text("Promotion Discount: -SGD %.2f".format(scenario.promotionDiscountCents / 100.0))
                if (scenario.giftItems.isNotEmpty()) {
                    Text("Gift Items: ${scenario.giftItems.joinToString()}")
                }
                Text(
                    text = "Payable: SGD %.2f".format(payableAmountCents / 100.0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Order Summary", style = MaterialTheme.typography.titleMedium)
                if (uiState.cartItems.isEmpty()) {
                    Text("QR order basket already synced from table code.")
                } else {
                    uiState.cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.product.name} x ${item.quantity}")
                            Text("SGD %.2f".format(item.lineAmountCents / 100.0))
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (scenario.source == "QR") "Cashier Payment Method" else "Payment Method",
                    style = MaterialTheme.typography.titleMedium
                )
                PaymentMethodOptionRow(
                    selectedMethod = uiState.selectedPaymentMethod,
                    primaryMethod = PaymentMethods.CASH,
                    secondaryMethod = PaymentMethods.CARD_TERMINAL,
                    onSelect = viewModel::selectPaymentMethod
                )
                PaymentMethodOptionRow(
                    selectedMethod = uiState.selectedPaymentMethod,
                    primaryMethod = PaymentMethods.WECHAT_QR,
                    secondaryMethod = PaymentMethods.ALIPAY_QR,
                    onSelect = viewModel::selectPaymentMethod
                )
                PaymentMethodOptionRow(
                    selectedMethod = uiState.selectedPaymentMethod,
                    primaryMethod = PaymentMethods.PAYNOW_QR,
                    secondaryMethod = null,
                    onSelect = viewModel::selectPaymentMethod
                )
                Text(
                    text = "Provider Status (${PaymentMethods.providerName(uiState.selectedPaymentMethod)}): ${uiState.paymentProviderStatus}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (scenario.source != "QR") {
                            viewModel.moveToPendingSettlement()
                        }
                        viewModel.preparePayment()
                        onStartPayment()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (uiState.cartItems.isNotEmpty() || scenario.source == "QR") &&
                        currentStage != ActiveOrderStage.SETTLED
                ) {
                    Text(
                        when {
                            scenario.source == "QR" -> "Collect Payment at Cashier"
                            currentStage == ActiveOrderStage.DRAFT -> "Move to Cashier Settlement"
                            currentStage == ActiveOrderStage.SUBMITTED -> "Open Cashier Settlement"
                            currentStage == ActiveOrderStage.PENDING_SETTLEMENT -> "Collect Payment at Cashier"
                            else -> "Order Already Settled"
                        }
                    )
                }

                if (scenario.source != "QR" && currentStage == ActiveOrderStage.DRAFT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = viewModel::sendToKitchen,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.cartItems.isNotEmpty()
                    ) {
                        Text("Send to Kitchen First")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodOptionRow(
    selectedMethod: String,
    primaryMethod: String,
    secondaryMethod: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PaymentMethodOption(
            modifier = Modifier.weight(1f),
            method = primaryMethod,
            selectedMethod = selectedMethod,
            onSelect = onSelect
        )
        if (secondaryMethod != null) {
            PaymentMethodOption(
                modifier = Modifier.weight(1f),
                method = secondaryMethod,
                selectedMethod = selectedMethod,
                onSelect = onSelect
            )
        } else {
            Spacer(modifier = Modifier.width(0.dp).weight(1f))
        }
    }
}

@Composable
private fun PaymentMethodOption(
    modifier: Modifier,
    method: String,
    selectedMethod: String,
    onSelect: (String) -> Unit
) {
    Row(modifier = modifier) {
        RadioButton(
            selected = selectedMethod == method,
            onClick = { onSelect(method) }
        )
        Text(PaymentMethods.displayName(method), modifier = Modifier.padding(top = 12.dp))
    }
}
