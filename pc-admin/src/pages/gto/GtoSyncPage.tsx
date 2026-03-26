import { Alert, Card, Col, Row, Skeleton, Table, Typography } from "antd";
import { getGtoBatches } from "../../api/services/gtoService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function GtoSyncPage() {
  const query = useAsyncData(getGtoBatches);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        GTO Sync
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        View mall GTO batch exports, sync status, and failed retry history.
      </Typography.Paragraph>

      {query.error ? <Alert type="error" message={query.error} /> : null}

      {query.loading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col span={6}>
              <Card>
                <Typography.Text type="secondary">Pending Batches</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  {(query.data ?? []).filter((item) => item.syncStatus === "PENDING").length}
                </Typography.Title>
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Typography.Text type="secondary">Failed Batches</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  {(query.data ?? []).filter((item) => item.syncStatus === "FAILED").length}
                </Typography.Title>
              </Card>
            </Col>
          </Row>

          <Card style={{ marginTop: 24 }}>
            <Table
              rowKey="id"
              dataSource={query.data ?? []}
              pagination={false}
              columns={[
                { title: "Batch No", dataIndex: "batchNo" },
                { title: "Business Date", dataIndex: "businessDate" },
                { title: "Store", dataIndex: "storeName" },
                { title: "Trades", dataIndex: "tradeCount" },
                { title: "Net Sales", dataIndex: "netSales" },
                { title: "Discounts", dataIndex: "discountAmount" },
                { title: "Status", dataIndex: "syncStatus" },
                { title: "Export Time", dataIndex: "exportTime" }
              ]}
            />
          </Card>
        </>
      )}
    </div>
  );
}
