package com.developer.pos.ui.screens.cashier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun CashierScreen(
    viewModel: CashierViewModel,
    onBack: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "POS Ordering",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search product or barcode") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1.2f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredProducts) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.addProduct(product) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    Text("Barcode: ${product.barcode}")
                                    Text("Stock: ${product.stockQty}")
                                }
                                Text("CNY %.2f".format(product.priceCents / 100.0))
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Card(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cart", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.cartItems.isEmpty()) {
                            Text("No items selected yet")
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.cartItems) { item ->
                                    Column {
                                        Text(item.product.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("CNY %.2f".format(item.product.priceCents / 100.0))
                                            Row {
                                                OutlinedButton(onClick = { viewModel.decreaseQuantity(item.product.id) }) {
                                                    Text("-")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${item.quantity}", modifier = Modifier.padding(top = 8.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                OutlinedButton(onClick = { viewModel.increaseQuantity(item.product.id) }) {
                                                    Text("+")
                                                }
                                            }
                                        }
                                        Text("Line total: CNY %.2f".format(item.lineAmountCents / 100.0))
                                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Items: ${uiState.totalItems}")
                        Text(
                            text = "Payable: CNY %.2f".format(uiState.payableAmountCents / 100.0),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onProceedToPayment,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.cartItems.isNotEmpty()
        ) {
            Text("Review Current Order")
        }
    }
}
