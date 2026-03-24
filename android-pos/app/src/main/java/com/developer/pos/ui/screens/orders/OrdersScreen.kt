package com.developer.pos.ui.screens.orders

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class OrderListItem(
    val orderNo: String,
    val tableCode: String,
    val source: String,
    val amount: String,
    val status: String,
    val payment: String,
    val time: String
)

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOpenOrderDetail: () -> Unit
) {
    val keyword = remember { mutableStateOf("") }
    val orders = listOf(
        OrderListItem("QR202603240019", "T2", "QR", "CNY 92.50", "PENDING_SETTLEMENT", "UNPAID", "18:42"),
        OrderListItem("QR202603240021", "T7", "QR", "CNY 71.00", "PENDING_SETTLEMENT", "UNPAID", "18:47"),
        OrderListItem("POS202603200001", "T4", "POS", "CNY 28.00", "PAID", "SDK_PAY", "09:21"),
        OrderListItem("POS202603200002", "T8", "POS", "CNY 12.00", "DRAFT", "CASH", "09:34"),
        OrderListItem("POS202603200003", "T1", "POS", "CNY 35.00", "REFUNDED", "SDK_PAY", "10:08")
    ).filter { keyword.value.isBlank() || it.orderNo.contains(keyword.value, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = keyword.value,
            onValueChange = { keyword.value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search order no") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenOrderDetail
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.orderNo, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Table: ${item.tableCode}")
                        Text("Source: ${item.source}")
                        Text("Amount: ${item.amount}")
                        Text("Status: ${item.status}")
                        Text("Payment: ${item.payment}")
                        Text("Time: ${item.time}")
                    }
                }
            }
        }
    }
}
