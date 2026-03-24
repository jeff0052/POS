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

@Composable
fun HomeScreen(
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
            text = "Store Home",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Revenue")
                Spacer(modifier = Modifier.height(6.dp))
                Text("CNY 0.00", style = MaterialTheme.typography.headlineMedium)
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
                Text("3 tables waiting for cashier settlement")
                Text("T2 · T7 · T13 synced from table code ordering")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStartCashier,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Cashier")
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
                Text("Settlement")
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
