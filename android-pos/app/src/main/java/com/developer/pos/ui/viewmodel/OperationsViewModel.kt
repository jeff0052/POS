package com.developer.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.pos.data.repository.PosOrderRepository
import com.developer.pos.ui.model.BackofficeOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OperationsViewModel @Inject constructor(
    private val posOrderRepository: PosOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperationsUiState())
    val uiState: StateFlow<OperationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                val dashboard = posOrderRepository.getDashboard()
                val orders = posOrderRepository.getMerchantOrders()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    dashboard = dashboard,
                    orders = orders,
                    selectedOrder = _uiState.value.selectedOrder?.let { selected ->
                        orders.find { it.orderId == selected.orderId } ?: orders.firstOrNull()
                    } ?: orders.firstOrNull()
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = error.message ?: "Failed to load operations"
                )
            }
        }
    }

    fun selectOrder(order: BackofficeOrder) {
        _uiState.value = _uiState.value.copy(selectedOrder = order)
    }
}
