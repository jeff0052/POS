package com.developer.pos.data.repository.impl

import com.developer.pos.data.remote.PosOrderApi
import com.developer.pos.data.remote.dto.CollectCashierSettlementRequestDto
import com.developer.pos.data.remote.dto.MerchantAdminOrderDto
import com.developer.pos.data.remote.dto.ReplaceActiveOrderItemDto
import com.developer.pos.data.remote.dto.ReplaceActiveOrderItemsRequestDto
import com.developer.pos.data.repository.PosDraftState
import com.developer.pos.data.repository.PosOrderRepository
import com.developer.pos.domain.model.CartItem
import com.developer.pos.ui.model.ActiveOrderStage
import com.developer.pos.ui.model.BackofficeDashboard
import com.developer.pos.ui.model.BackofficeOrder
import com.developer.pos.ui.model.BackofficeOrderItem
import com.developer.pos.ui.model.PaymentScenario
import javax.inject.Inject

private const val STORE_ID = 101L
private const val TABLE_ID = 10002L
private const val TABLE_CODE = "T2"
private const val CASHIER_ID = 9001L

class PosOrderRepositoryImpl @Inject constructor(
    private val posOrderApi: PosOrderApi
) : PosOrderRepository {

    override suspend fun getDashboard(): BackofficeDashboard {
        val daily = posOrderApi.getDailySummary().data
        val sales = posOrderApi.getSalesSummary().data
        val orders = posOrderApi.getMerchantOrders().data

        return BackofficeDashboard(
            totalRevenueCents = daily.totalRevenueCents,
            orderCount = daily.orderCount,
            refundAmountCents = daily.refundAmountCents,
            rechargeSalesCents = sales.rechargeSalesCents,
            totalDiscountCents = sales.totalDiscountCents,
            pendingQrTables = orders
                .filter { it.orderType == "QR" && it.orderStatus == "PENDING_SETTLEMENT" }
                .mapNotNull { it.tableCode }
                .distinct()
        )
    }

    override suspend fun getMerchantOrders(): List<BackofficeOrder> {
        return posOrderApi.getMerchantOrders().data.map { it.toModel() }
    }

    override suspend fun syncDraft(cartItems: List<CartItem>): PosDraftState {
        val response = posOrderApi.replaceItems(
            storeId = STORE_ID,
            tableId = TABLE_ID,
            request = ReplaceActiveOrderItemsRequestDto(
                orderSource = "POS",
                memberId = null,
                items = cartItems.map { item ->
                    ReplaceActiveOrderItemDto(
                        skuId = item.product.id,
                        skuCode = item.product.barcode ?: "sku-${item.product.id}",
                        skuName = item.product.name,
                        quantity = item.quantity,
                        unitPriceCents = item.product.priceCents,
                        remark = null
                    )
                }
            )
        ).data

        return PosDraftState(
            activeOrderId = response.activeOrderId,
            orderNo = response.orderNo,
            stage = ActiveOrderStage.valueOf(response.status),
            payableAmountCents = response.payableAmountCents
        )
    }

    override suspend fun sendToKitchen(activeOrderId: String): ActiveOrderStage {
        val result = posOrderApi.submitToKitchen(STORE_ID, TABLE_ID, activeOrderId).data
        return ActiveOrderStage.valueOf(result.status)
    }

    override suspend fun moveToPaymentPending(): PaymentScenario {
        posOrderApi.moveToPayment(STORE_ID, TABLE_ID)
        val preview = posOrderApi.getPaymentPreview(STORE_ID, TABLE_ID).data
        return PaymentScenario(
            source = "POS",
            tableCode = TABLE_CODE,
            memberName = preview.member?.memberName,
            memberTier = preview.member?.memberTier,
            originalAmountCents = preview.pricing.originalAmountCents,
            memberDiscountCents = preview.pricing.memberDiscountCents,
            promotionDiscountCents = preview.pricing.promotionDiscountCents,
            payableAmountCents = preview.pricing.payableAmountCents,
            giftItems = preview.giftItems.map { "${it.skuName} x${it.quantity}" },
            headline = "Active table order ready for cashier settlement"
        )
    }

    override suspend fun collectPayment(paymentMethod: String, collectedAmountCents: Long): ActiveOrderStage {
        val result = posOrderApi.collectPayment(
            STORE_ID,
            TABLE_ID,
            CollectCashierSettlementRequestDto(
                cashierId = CASHIER_ID,
                paymentMethod = paymentMethod,
                collectedAmountCents = collectedAmountCents
            )
        ).data
        return ActiveOrderStage.valueOf(result.finalStatus)
    }

    private fun MerchantAdminOrderDto.toModel(): BackofficeOrder {
        return BackofficeOrder(
            orderId = orderId,
            orderNo = orderNo,
            tableCode = tableCode,
            orderType = orderType,
            orderStatus = orderStatus,
            paymentMethod = paymentMethod,
            createdAt = createdAt,
            memberName = memberName,
            memberTier = memberTier,
            originalAmountCents = originalAmountCents,
            memberDiscountCents = memberDiscountCents,
            promotionDiscountCents = promotionDiscountCents,
            payableAmountCents = payableAmountCents,
            giftItems = giftItems,
            items = items.map { item ->
                BackofficeOrderItem(
                    productName = item.productName,
                    quantity = item.quantity,
                    amountCents = item.amountCents,
                    originalAmountCents = item.originalAmountCents,
                    memberBenefitCents = item.memberBenefitCents,
                    promotionBenefitCents = item.promotionBenefitCents,
                    gift = item.gift
                )
            }
        )
    }
}
