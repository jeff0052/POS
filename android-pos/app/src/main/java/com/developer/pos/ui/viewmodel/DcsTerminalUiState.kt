package com.developer.pos.ui.viewmodel

data class DcsTerminalUiState(
    val loading: Boolean = false,
    val providerStatus: String = "Checking DCS terminal",
    val terminalConnected: Boolean = false,
    val lastActionLabel: String? = null,
    val lastActionSuccess: Boolean = false,
    val lastActionMessage: String? = null
)
