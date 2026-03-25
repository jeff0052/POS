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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.model.BackofficeOrder
import com.developer.pos.ui.viewmodel.OperationsUiState

@Composable
fun OrdersScreen(
    uiState: OperationsUiState,
    onBack: () -> Unit,
    onOpenOrderDetail: (BackofficeOrder) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    val orders = uiState.orders.filter { keyword.isBlank() || it.orderNo.contains(keyword, ignoreCase = true) }

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
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search order no") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.loading) {
            Text("Loading active orders...")
            Spacer(modifier = Modifier.height(12.dp))
        }
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders) { item ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenOrderDetail(item) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.orderNo, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Table: ${item.tableCode ?: "-"}")
                        Text("Source: ${item.orderType}")
                        Text("Amount: CNY %.2f".format(item.payableAmountCents / 100.0))
                        Text("Status: ${item.orderStatus}")
                        Text("Payment: ${item.paymentMethod ?: "UNPAID"}")
                        Text("Time: ${item.createdAt}")
                    }
                }
            }
        }
    }
}
