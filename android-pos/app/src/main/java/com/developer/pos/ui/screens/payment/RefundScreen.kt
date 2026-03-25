package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.device.payment.PaymentMethods
import com.developer.pos.ui.model.BackofficeOrder
import com.developer.pos.ui.viewmodel.DcsTerminalUiState

@Composable
fun RefundScreen(
    order: BackofficeOrder?,
    uiState: DcsTerminalUiState,
    onBack: () -> Unit,
    onVoidSale: () -> Unit,
    onSubmitRefund: () -> Unit
) {
    val reason = remember { mutableStateOf("") }
    val currentOrder = order
    val isCardOrder = currentOrder?.paymentMethod == PaymentMethods.CARD_TERMINAL

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
            Text("Refund", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentOrder == null) {
                    Text("No order selected")
                } else {
                    Text("Order No: ${currentOrder.orderNo}")
                    Text("Payment Method: ${currentOrder.paymentMethod ?: "UNPAID"}")
                    Text("Refund Amount: CNY %.2f".format(currentOrder.payableAmountCents / 100.0))
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = reason.value,
                    onValueChange = { reason.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Refund reason") }
                )
                if (!isCardOrder) {
                    Text(
                        "DCS refund and void are only available for Card Terminal orders.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.lastActionLabel?.let { label ->
                    Text(
                        "$label: ${uiState.lastActionMessage ?: if (uiState.lastActionSuccess) "Completed" else "Failed"}",
                        color = if (uiState.lastActionSuccess) Color(0xFF147D39) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onVoidSale,
                modifier = Modifier.weight(1f),
                enabled = !uiState.loading && currentOrder != null && isCardOrder
            ) {
                Text("Void Sale")
            }
            Button(
                onClick = onSubmitRefund,
                modifier = Modifier.weight(1f),
                enabled = !uiState.loading && currentOrder != null && isCardOrder
            ) {
                Text(if (uiState.loading) "Processing..." else "Submit Refund")
            }
        }
    }
}
