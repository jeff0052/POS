import { Button, Descriptions, Divider, Drawer, Space, Tag, Typography } from "antd";
import type { RefundRecord } from "../../../types";

export function RefundDetailDrawer({
  refund,
  open,
  onClose
}: {
  refund: RefundRecord | null;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <Drawer title={refund?.refundNo ?? "Refund Detail"} open={open} onClose={onClose} width={500}>
      {refund ? (
        <>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Refund No">{refund.refundNo}</Descriptions.Item>
            <Descriptions.Item label="Order No">{refund.orderNo}</Descriptions.Item>
            <Descriptions.Item label="Amount">{refund.refundAmount}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={refund.status === "SUCCESS" ? "green" : refund.status === "FAILED" ? "red" : "gold"}>
                {refund.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Operator">{refund.operator}</Descriptions.Item>
            <Descriptions.Item label="Time">{refund.time}</Descriptions.Item>
          </Descriptions>
          <Divider />
          <Typography.Title level={5} style={{ marginTop: 0 }}>
            Refund Trace
          </Typography.Title>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="Original Order">{refund.orderNo}</Descriptions.Item>
            <Descriptions.Item label="Processing Note">
              Refund records should stay traceable even when the original payment trace is reviewed elsewhere.
            </Descriptions.Item>
          </Descriptions>
          <Divider />
          <Space>
            <Button type="primary">Print Refund Slip Later</Button>
            <Button>Escalate Later</Button>
          </Space>
        </>
      ) : null}
    </Drawer>
  );
}
