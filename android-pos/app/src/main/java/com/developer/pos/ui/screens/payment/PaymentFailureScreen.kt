package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PaymentFailureScreen(
    viewModel: CashierViewModel,
    onRetry: () -> Unit,
    onBackToCashier: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cashier Settlement Failed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Order No: ${uiState.currentOrderNo}")
                Text("Please retry cashier settlement after payment SDK integration or network recovery.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                Text("Retry Settlement")
            }
            OutlinedButton(onClick = onBackToCashier, modifier = Modifier.weight(1f)) {
                Text("Back to POS Ordering")
            }
        }
    }
}
