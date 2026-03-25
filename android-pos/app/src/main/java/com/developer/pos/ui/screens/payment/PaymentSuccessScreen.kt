package com.developer.pos.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.developer.pos.ui.model.PaymentScenarioStore
import com.developer.pos.ui.viewmodel.CashierViewModel

@Composable
fun PaymentSuccessScreen(
    viewModel: CashierViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario = PaymentScenarioStore.current
    val payableAmountCents = if (scenario.payableAmountCents > 0L) scenario.payableAmountCents else uiState.payableAmountCents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Payment Completed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Order No: ${uiState.currentOrderNo}")
                Text("Source: ${scenario.source}${scenario.tableCode?.let { " · $it" } ?: ""}")
                Text("Order Stage: ${uiState.activeOrderStage.label}")
                Text("Amount: CNY %.2f".format(payableAmountCents / 100.0))
                Text(
                    if (scenario.memberTier != null) {
                        "Member: ${scenario.memberName} / ${scenario.memberTier}"
                    } else {
                        "Member: ${scenario.memberName ?: "Guest"}"
                    }
                )
                Text("Member Discount: -CNY %.2f".format(scenario.memberDiscountCents / 100.0))
                Text("Promotion Discount: -CNY %.2f".format(scenario.promotionDiscountCents / 100.0))
                if (scenario.giftItems.isNotEmpty()) {
                    Text("Gift Items: ${scenario.giftItems.joinToString()}")
                }
                Text(
                    if (scenario.source == "QR") {
                        "This QR table order has been settled by cashier. Receipt printing will be wired after printer SDK integration."
                    } else {
                        "This active table order has been settled. Receipt printing will be wired after printer SDK integration."
                    }
                )
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish")
        }
    }
}
