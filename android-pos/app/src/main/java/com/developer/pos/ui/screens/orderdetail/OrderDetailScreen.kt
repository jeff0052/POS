package com.developer.pos.ui.screens.orderdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.model.BackofficeOrder

@Composable
fun OrderDetailScreen(
    order: BackofficeOrder?,
    onBack: () -> Unit,
    onRefund: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val currentOrder = order

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
                if (currentOrder == null) {
                    Text("No order selected")
                } else {
                    Text("Order No: ${currentOrder.orderNo}")
                    Text("Table: ${currentOrder.tableCode ?: "-"}")
                    Text("Source: ${currentOrder.orderType}")
                    Text("Amount: CNY %.2f".format(currentOrder.payableAmountCents / 100.0))
                    Text("Status: ${currentOrder.orderStatus}")
                    Text("Payment: ${currentOrder.paymentMethod ?: "UNPAID"}")
                    Text("Member: ${currentOrder.memberName ?: "Walk-in"}${currentOrder.memberTier?.let { " / $it" } ?: ""}")
                    Text("Payment: ${if (currentOrder.orderStatus == "PENDING_SETTLEMENT") "Payment Pending" else currentOrder.orderStatus}")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Items", style = MaterialTheme.typography.titleMedium)
                if (currentOrder == null) {
                    Text("No item lines available")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(180.dp)) {
                        items(currentOrder.items) { item ->
                            Text("${item.productName} x ${item.quantity}  |  CNY %.2f".format(item.amountCents / 100.0))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Original Amount: CNY %.2f".format(currentOrder.originalAmountCents / 100.0))
                    Text("Member Discount: -CNY %.2f".format(currentOrder.memberDiscountCents / 100.0))
                    Text("Promotion Discount: -CNY %.2f".format(currentOrder.promotionDiscountCents / 100.0))
                    if (currentOrder.giftItems.isNotEmpty()) {
                        Text("Gift Items: ${currentOrder.giftItems.joinToString(", ")}")
                    }
                    Text("Payable: CNY %.2f".format(currentOrder.payableAmountCents / 100.0), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onProceedToPayment,
                modifier = Modifier.weight(1f),
                enabled = currentOrder?.orderStatus == "PENDING_SETTLEMENT"
            ) {
                Text("Open Cashier Settlement")
            }
            OutlinedButton(
                onClick = onRefund,
                modifier = Modifier.weight(1f),
                enabled = currentOrder?.paymentMethod == "CARD_TERMINAL"
            ) {
                Text("Card Refund / Void")
            }
        }
    }
}
