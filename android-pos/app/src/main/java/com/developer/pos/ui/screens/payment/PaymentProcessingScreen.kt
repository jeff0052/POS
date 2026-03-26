package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.developer.pos.device.payment.PaymentMethods
import com.developer.pos.ui.model.PaymentScenarioStore
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PaymentProcessingScreen(
    viewModel: CashierViewModel,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario = PaymentScenarioStore.current
    val payableAmountCents = if (scenario.payableAmountCents > 0L) scenario.payableAmountCents else uiState.payableAmountCents

    LaunchedEffect(Unit) {
        viewModel.startSelectedPayment(
            onSuccess = onPaymentSuccess,
            onFailure = onPaymentFailure
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    if (PaymentMethods.isQr(uiState.selectedPaymentMethod)) {
                        "Preparing QR Payment"
                    } else {
                        "Processing Cashier Settlement"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Order No: ${uiState.currentOrderNo}")
                Text("Payment Method: ${PaymentMethods.displayName(uiState.selectedPaymentMethod)}")
                Text("Amount: SGD %.2f".format(payableAmountCents / 100.0))
                Text("Current Stage: ${uiState.activeOrderStage.label}")
                Text("Provider Status: ${uiState.paymentProviderStatus}", style = MaterialTheme.typography.bodyMedium)
                uiState.paymentErrorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (PaymentMethods.isQr(uiState.selectedPaymentMethod)) {
                    Text("The terminal is creating a VibeCash payment link for the customer.")
                }
            }
        }
    }
}
