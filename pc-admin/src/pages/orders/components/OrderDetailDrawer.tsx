import { Button, Descriptions, Divider, Drawer, List, Space, Tag, Typography } from "antd";
import type { Order } from "../../../types";

export function OrderDetailDrawer({
  order,
  open,
  onClose
}: {
  order: Order | null;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <Drawer title={order?.orderNo ?? "Order Detail"} open={open} onClose={onClose} width={520}>
      {order ? (
        <>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Amount">{order.amount}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={order.status === "PAID" ? "green" : "gold"}>{order.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Payment">{order.payment}</Descriptions.Item>
            <Descriptions.Item label="Cashier">{order.cashier}</Descriptions.Item>
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
            <Descriptions.Item label="Current Status">{order.status}</Descriptions.Item>
            <Descriptions.Item label="Print Status">{order.printStatus}</Descriptions.Item>
            <Descriptions.Item label="Trace Note">
              Payment success and print result are tracked independently.
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
                  description={item.amount}
                />
              </List.Item>
            )}
          />
        </>
      ) : null}
    </Drawer>
  );
}
