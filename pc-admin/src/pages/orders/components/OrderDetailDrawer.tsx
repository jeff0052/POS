import { Button, Descriptions, Divider, Drawer, List, Space, Tag, Typography } from "antd";
import type { Order } from "../../../types";

function getOrderStatusMeta(status: Order["status"]) {
  switch (status) {
    case "DRAFT":
      return { label: "Draft", color: "default" };
    case "SUBMITTED":
      return { label: "Sent to Kitchen", color: "processing" };
    case "PENDING_SETTLEMENT":
      return { label: "Pending Settlement", color: "gold" };
    case "PAID":
      return { label: "Paid", color: "green" };
    case "REFUNDED":
      return { label: "Refunded", color: "red" };
    default:
      return { label: status, color: "default" };
  }
}

export function OrderDetailDrawer({
  order,
  open,
  onClose
}: {
  order: Order | null;
  open: boolean;
  onClose: () => void;
}) {
  const statusMeta = order ? getOrderStatusMeta(order.status) : null;
  return (
    <Drawer title={order?.orderNo ?? "Order Detail"} open={open} onClose={onClose} width={520}>
      {order ? (
        <>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Table">{order.tableCode ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Order Type">{order.orderType ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Amount">{order.amount}</Descriptions.Item>
            <Descriptions.Item label="Original Amount">{order.originalAmount ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Member Discount">{order.memberDiscount ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Promotion Discount">{order.promotionDiscount ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="Payable">{order.payableAmount ?? order.amount}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={statusMeta?.color}>
                {statusMeta?.label}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Payment">{order.payment}</Descriptions.Item>
            <Descriptions.Item label="Cashier">{order.cashier}</Descriptions.Item>
            <Descriptions.Item label="Member">
              {order.memberName ? `${order.memberName} / ${order.memberTier ?? "-"}` : "-"}
            </Descriptions.Item>
            <Descriptions.Item label="Print Status">{order.printStatus}</Descriptions.Item>
            <Descriptions.Item label="Time">{order.time}</Descriptions.Item>
          </Descriptions>
          <Divider />
          <Typography.Title level={5} style={{ marginTop: 0 }}>
            Payment Trace
          </Typography.Title>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Order No">{order.orderNo}</Descriptions.Item>
            <Descriptions.Item label="Payment Method">{order.payment}</Descriptions.Item>
            <Descriptions.Item label="Current Status">{statusMeta?.label ?? order.status}</Descriptions.Item>
            <Descriptions.Item label="Print Status">{order.printStatus}</Descriptions.Item>
            <Descriptions.Item label="Gift Items">
              {order.giftItems?.length ? order.giftItems.join(", ") : "-"}
            </Descriptions.Item>
            <Descriptions.Item label="Trace Note">
              Payment, member settlement, promotion hit records, and print result are tracked independently.
            </Descriptions.Item>
          </Descriptions>
          <Divider />
          <Space>
            <Button type="primary">Reprint Later</Button>
            <Button>Refund Later</Button>
          </Space>
          <List
            style={{ marginTop: 16 }}
            header="Items"
            bordered
            dataSource={order.items}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  title={`${item.productName} x ${item.quantity}`}
                  description={[
                    item.amount,
                    item.memberBenefit,
                    item.promotionBenefit,
                    item.gift ? "Gift item" : undefined
                  ]
                    .filter(Boolean)
                    .join(" · ")}
                />
              </List.Item>
            )}
          />
        </>
      ) : null}
    </Drawer>
  );
}
