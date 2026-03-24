package com.developer.pos.ui.screens.orderdetail

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onRefund: () -> Unit,
    onProceedToPayment: () -> Unit
) {
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
            Text("Active Table Order", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Order No: QR202603240019")
                Text("Table: T2")
                Text("Source: QR table order")
                Text("Amount: CNY 92.50")
                Text("Status: PENDING_SETTLEMENT")
                Text("Payment: UNPAID")
                Text("Member: Lina Chen / Gold")
                Text("Cashier Settlement: Pending Settlement")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Items", style = MaterialTheme.typography.titleMedium)
                Text("Black Pepper Beef x 1  |  CNY 31.00")
                Text("Peach Soda x 2  |  CNY 20.00")
                Text("Signature Fried Rice x 1  |  CNY 16.00")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Original Amount: CNY 100.50")
                Text("Member Discount: -CNY 6.00")
                Text("Promotion Discount: -CNY 2.00")
                Text("Payable: CNY 92.50", fontWeight = FontWeight.SemiBold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onProceedToPayment, modifier = Modifier.weight(1f)) {
                Text("Open Cashier Settlement")
            }
            OutlinedButton(onClick = onRefund, modifier = Modifier.weight(1f)) {
                Text("Refund")
            }
        }
    }
}
