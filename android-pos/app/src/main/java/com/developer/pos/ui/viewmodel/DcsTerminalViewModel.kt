package com.developer.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.pos.device.payment.DcsPaymentService
import com.developer.pos.device.payment.PaymentMethods
import com.developer.pos.ui.model.BackofficeOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DcsTerminalViewModel @Inject constructor(
    private val dcsPaymentService: DcsPaymentService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DcsTerminalUiState())
    val uiState: StateFlow<DcsTerminalUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            runCatching {
                dcsPaymentService.connect(PaymentMethods.CARD_TERMINAL)
            }.onSuccess { status ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    providerStatus = status.message,
                    terminalConnected = status.connected
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    providerStatus = error.message ?: "Failed to connect DCS terminal",
                    terminalConnected = false
                )
            }
        }
    }

    fun runTerminalSettlement(onComplete: (() -> Unit)? = null) {
        runAction("Terminal Settlement", onComplete) {
            dcsPaymentService.terminalSettlement()
        }
    }

    fun signOffTerminal(onComplete: (() -> Unit)? = null) {
        runAction("Terminal Sign Off", onComplete) {
            dcsPaymentService.signOffTerminal()
        }
    }

    fun voidSale(order: BackofficeOrder, onComplete: (() -> Unit)? = null) {
        runAction("Card Void", onComplete) {
            dcsPaymentService.voidSale(
                originalOrderNo = order.orderNo,
                voidOrderNo = buildFollowUpOrderNo(order.orderNo, "VOID")
            )
        }
    }

    fun refundSale(order: BackofficeOrder, onComplete: (() -> Unit)? = null) {
        runAction("Card Refund", onComplete) {
            dcsPaymentService.refundSale(
                originalOrderNo = order.orderNo,
                refundOrderNo = buildFollowUpOrderNo(order.orderNo, "REFUND"),
                amountCents = order.payableAmountCents
            )
        }
    }

    fun clearLastAction() {
        _uiState.value = _uiState.value.copy(
            lastActionLabel = null,
            lastActionMessage = null
        )
    }

    private fun runAction(
        label: String,
        onComplete: (() -> Unit)? = null,
        action: suspend () -> com.developer.pos.device.payment.PaymentResult
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, lastActionLabel = label, lastActionMessage = null)
            runCatching { action() }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        terminalConnected = true,
                        providerStatus = result.message ?: "$label completed",
                        lastActionLabel = label,
                        lastActionSuccess = result.success,
                        lastActionMessage = result.message ?: if (result.success) "$label completed" else "$label failed"
                    )
                    if (result.success) {
                        onComplete?.invoke()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        terminalConnected = false,
                        providerStatus = error.message ?: "$label failed",
                        lastActionLabel = label,
                        lastActionSuccess = false,
                        lastActionMessage = error.message ?: "$label failed"
                    )
                }
        }
    }

    private fun buildFollowUpOrderNo(originalOrderNo: String, action: String): String {
        return "${originalOrderNo.take(24)}-$action-${System.currentTimeMillis().toString().takeLast(6)}"
    }
}
