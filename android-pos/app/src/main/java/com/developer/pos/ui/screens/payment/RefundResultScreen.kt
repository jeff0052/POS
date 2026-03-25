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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.viewmodel.DcsTerminalUiState

@Composable
fun RefundResultScreen(
    uiState: DcsTerminalUiState,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    uiState.lastActionLabel ?: "DCS Result",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.lastActionSuccess) Color(0xFF147D39) else MaterialTheme.colorScheme.error
                )
                Text(uiState.lastActionMessage ?: "No DCS action result is available.")
                Text("Terminal status: ${uiState.providerStatus}")
                Text("Slip printing will be wired after printer SDK integration.")
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
