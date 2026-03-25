package com.developer.pos.ui.model

enum class ActiveOrderStage(val label: String) {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    PENDING_SETTLEMENT("PENDING_SETTLEMENT"),
    SETTLED("SETTLED")
}
