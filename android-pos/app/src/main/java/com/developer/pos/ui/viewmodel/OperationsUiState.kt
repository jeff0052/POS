package com.developer.pos.ui.viewmodel

import com.developer.pos.ui.model.BackofficeDashboard
import com.developer.pos.ui.model.BackofficeOrder

data class OperationsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dashboard: BackofficeDashboard = BackofficeDashboard(),
    val orders: List<BackofficeOrder> = emptyList(),
    val selectedOrder: BackofficeOrder? = null
)
