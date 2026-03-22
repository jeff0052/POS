package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PaymentConfirmScreen(
    viewModel: CashierViewModel,
    onBack: () -> Unit,
    onStartPayment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Payment Confirm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Order Summary", style = MaterialTheme.typography.titleMedium)
                uiState.cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.product.name} x ${item.quantity}")
                        Text("CNY %.2f".format(item.lineAmountCents / 100.0))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total: CNY %.2f".format(uiState.payableAmountCents / 100.0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        RadioButton(
                            selected = uiState.selectedPaymentMethod == "CASH",
                            onClick = { viewModel.selectPaymentMethod("CASH") }
                        )
                        Text("Cash", modifier = Modifier.padding(top = 12.dp))
                    }
                    Row {
                        RadioButton(
                            selected = uiState.selectedPaymentMethod == "SDK_PAY",
                            onClick = { viewModel.selectPaymentMethod("SDK_PAY") }
                        )
                        Text("SDK Pay", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.prepareMockPayment()
                onStartPayment()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.cartItems.isNotEmpty()
        ) {
            Text("Create Order and Start Payment")
        }
    }
}
