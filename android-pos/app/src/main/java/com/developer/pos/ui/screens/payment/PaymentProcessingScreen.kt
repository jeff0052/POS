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
import com.developer.pos.ui.viewmodel.CashierViewModel
import kotlinx.coroutines.delay

@Composable
fun PaymentProcessingScreen(
    viewModel: CashierViewModel,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(1200)
        onPaymentSuccess()
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
                Text("Processing Cashier Settlement", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Order No: ${uiState.currentOrderNo}")
                Text("Payment Method: ${uiState.selectedPaymentMethod}")
                Text("Amount: CNY %.2f".format(uiState.payableAmountCents / 100.0))
                Text(
                    "Mock mode is enabled. Payment currently defaults to success until the real SDK is integrated.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
