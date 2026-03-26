import { apiGetV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Order } from "../../types";

interface OrderListResponseItem {
    orderId: string;
    orderNo: string;
    storeId: number;
    tableId?: number;
    tableCode?: string;
    orderType?: "POS" | "QR";
    orderStatus: "DRAFT" | "SUBMITTED" | "PENDING_SETTLEMENT" | "PAID" | "REFUNDED";
    paymentMethod?: "CASH" | "CARD_TERMINAL" | "WECHAT_QR" | "ALIPAY_QR" | "PAYNOW_QR" | "UNPAID";
    createdAt: string;
    memberName?: string | null;
    memberTier?: string | null;
    originalAmountCents: number;
    memberDiscountCents: number;
    promotionDiscountCents: number;
    payableAmountCents: number;
    giftItems?: string[];
    items: Array<{
      productName: string;
      quantity: number;
      amountCents: number;
      originalAmountCents: number;
      memberBenefitCents: number;
      promotionBenefitCents: number;
      gift: boolean;
    }>;
}

type OrderListResponse = OrderListResponseItem[];

function formatCurrency(amountCents: number) {
  return `SGD ${(amountCents / 100).toFixed(2)}`;
}

export async function getOrders(): Promise<Order[]> {
  if (USE_MOCK_API) {
    return mockApi.getOrders();
  }

  const response = await apiGetV2<OrderListResponse>("/admin/orders?storeId=101");
  return response.map((item, index) => ({
    id: index + 1,
    orderId: item.orderId,
    orderNo: item.orderNo,
    amount: formatCurrency(item.payableAmountCents),
    status: item.orderStatus,
    payment: item.paymentMethod ?? "UNPAID",
    time: item.createdAt,
    cashier: item.orderType === "QR" ? "QR guest" : "Store cashier",
    printStatus: "NOT_PRINTED",
    items: item.items.map((orderItem) => ({
      productName: orderItem.productName,
      quantity: orderItem.quantity,
      amount: formatCurrency(orderItem.amountCents),
      originalAmount: formatCurrency(orderItem.originalAmountCents),
      memberBenefit: orderItem.memberBenefitCents ? formatCurrency(orderItem.memberBenefitCents) : undefined,
      promotionBenefit: orderItem.promotionBenefitCents
        ? formatCurrency(orderItem.promotionBenefitCents)
        : undefined,
      gift: orderItem.gift
    })),
    storeId: item.storeId,
    tableId: item.tableId,
    tableCode: item.tableCode,
    orderType: item.orderType,
    memberName: item.memberName ?? undefined,
    memberTier: item.memberTier ?? undefined,
    originalAmount: formatCurrency(item.originalAmountCents),
    memberDiscount: item.memberDiscountCents ? formatCurrency(item.memberDiscountCents) : undefined,
    promotionDiscount: item.promotionDiscountCents ? formatCurrency(item.promotionDiscountCents) : undefined,
    payableAmount: formatCurrency(item.payableAmountCents),
    giftItems: item.giftItems ?? []
  }));
}
