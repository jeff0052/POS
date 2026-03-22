import { Alert, Card, Input, Select, Space, Table, Tag, Typography } from "antd";
import { useMemo, useState } from "react";
import { getRefunds } from "../../api/services/refundService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { RefundRecord } from "../../types";
import { RefundDetailDrawer } from "./components/RefundDetailDrawer";

const columns = [
  { title: "Refund No", dataIndex: "refundNo", key: "refundNo" },
  { title: "Order No", dataIndex: "orderNo", key: "orderNo" },
  { title: "Amount", dataIndex: "refundAmount", key: "refundAmount" },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: string) => {
      const color = value === "SUCCESS" ? "green" : value === "FAILED" ? "red" : "gold";
      return <Tag color={color}>{value}</Tag>;
    }
  },
  { title: "Operator", dataIndex: "operator", key: "operator" },
  { title: "Time", dataIndex: "time", key: "time" }
];

export function RefundsPage() {
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string | undefined>();
  const [selectedRefund, setSelectedRefund] = useState<RefundRecord | null>(null);
  const query = useAsyncData(getRefunds);

  const data = useMemo(() => {
    let list = query.data ?? [];
    if (keyword.trim()) {
      list = list.filter(
        (item) =>
          item.refundNo.toLowerCase().includes(keyword.toLowerCase()) ||
          item.orderNo.toLowerCase().includes(keyword.toLowerCase())
      );
    }
    if (status) {
      list = list.filter((item) => item.status === status);
    }
    return list;
  }, [query.data, keyword, status]);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Refunds
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        退款记录页，用来排查退款状态、操作人和订单关联关系。
      </Typography.Paragraph>

      <Card>
        <Space wrap>
          <Input
            placeholder="Refund No / Order No"
            style={{ width: 260 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <Select
            placeholder="Status"
            allowClear
            style={{ width: 180 }}
            onChange={(value) => setStatus(value)}
            options={[
              { label: "Processing", value: "PROCESSING" },
              { label: "Success", value: "SUCCESS" },
              { label: "Failed", value: "FAILED" }
            ]}
          />
        </Space>
        {query.error ? <Alert type="error" message={query.error} style={{ marginTop: 16 }} /> : null}
        <Table
          style={{ marginTop: 16 }}
          rowKey="refundNo"
          columns={columns}
          loading={query.loading}
          dataSource={data}
          locale={{ emptyText: "No refunds yet" }}
          onRow={(record) => ({
            onClick: () => setSelectedRefund(record)
          })}
        />
      </Card>
      <RefundDetailDrawer
        refund={selectedRefund}
        open={Boolean(selectedRefund)}
        onClose={() => setSelectedRefund(null)}
      />
    </div>
  );
}
