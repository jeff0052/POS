package com.developer.pos.ui.screens.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.pos.ui.viewmodel.DcsTerminalUiState

@Composable
fun SettlementScreen(
    uiState: DcsTerminalUiState,
    onBack: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRunSettlement: () -> Unit,
    onSignOff: () -> Unit
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
            Text("Cashier Settlement Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DCS Terminal Operations", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (uiState.terminalConnected) "Terminal Connected" else "Terminal Not Ready",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (uiState.terminalConnected) Color(0xFF147D39) else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.providerStatus)
                uiState.lastActionLabel?.let { label ->
                    Text(
                        "$label: ${uiState.lastActionMessage ?: if (uiState.lastActionSuccess) "Completed" else "Failed"}",
                        color = if (uiState.lastActionSuccess) Color(0xFF147D39) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefreshStatus,
                modifier = Modifier.weight(1f),
                enabled = !uiState.loading
            ) {
                Text("Refresh")
            }
            OutlinedButton(
                onClick = onSignOff,
                modifier = Modifier.weight(1f),
                enabled = !uiState.loading
            ) {
                Text("Sign Off")
            }
        }

        Button(
            onClick = onRunSettlement,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.loading
        ) {
            Text(if (uiState.loading) "Processing..." else "Run Terminal Settlement")
        }
    }
}
