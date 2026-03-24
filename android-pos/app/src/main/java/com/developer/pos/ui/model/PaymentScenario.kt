package com.developer.pos.ui.model

data class PaymentScenario(
    val source: String,
    val tableCode: String?,
    val memberName: String?,
    val memberTier: String?,
    val originalAmountCents: Long,
    val memberDiscountCents: Long,
    val promotionDiscountCents: Long,
    val payableAmountCents: Long,
    val giftItems: List<String>,
    val headline: String
) {
    companion object {
        fun fromPos(payableAmountCents: Long): PaymentScenario = PaymentScenario(
            source = "POS",
            tableCode = "T4",
            memberName = "Walk-in Guest",
            memberTier = null,
            originalAmountCents = payableAmountCents,
            memberDiscountCents = 0L,
            promotionDiscountCents = 0L,
            payableAmountCents = payableAmountCents,
            giftItems = emptyList(),
            headline = "Counter checkout"
        )

        fun qrDemo(): PaymentScenario = PaymentScenario(
            source = "QR",
            tableCode = "T2",
            memberName = "Lina Chen",
            memberTier = "Gold",
            originalAmountCents = 10050L,
            memberDiscountCents = 600L,
            promotionDiscountCents = 200L,
            payableAmountCents = 9250L,
            giftItems = listOf("Peach Soda"),
            headline = "QR table settlement"
        )
    }
}

object PaymentScenarioStore {
    var current: PaymentScenario = PaymentScenario.fromPos(0L)
}
