package com.developer.pos.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.viewmodel.OperationsUiState

@Composable
fun HomeScreen(
    uiState: OperationsUiState,
    onStartCashier: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSettlement: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Restaurant POS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (uiState.loading) {
            Text("Loading live store operations...")
        }
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Revenue")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "SGD %.2f".format(uiState.dashboard.totalRevenueCents / 100.0),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Device Status")
                Spacer(modifier = Modifier.height(6.dp))
                Text("Payment SDK: Not connected")
                Text("Printer SDK: Not connected")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("QR Table Orders")
                Spacer(modifier = Modifier.height(6.dp))
                Text("${uiState.dashboard.pendingQrTables.size} tables in Payment Pending")
                Text(
                    if (uiState.dashboard.pendingQrTables.isEmpty()) {
                        "No QR tables waiting for cashier"
                    } else {
                        uiState.dashboard.pendingQrTables.joinToString(" · ")
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStartCashier,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open POS Ordering")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onOpenOrders,
                modifier = Modifier.weight(1f)
            ) {
                Text("Orders")
            }
            OutlinedButton(
                onClick = onOpenSettlement,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cashier Settlement")
            }
        }
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }
    }
}
