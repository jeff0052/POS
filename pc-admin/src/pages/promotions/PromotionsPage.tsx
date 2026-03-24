import { Alert, Card, Col, Row, Skeleton, Table, Typography } from "antd";
import { getPromotionRules } from "../../api/services/promotionService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function PromotionsPage() {
  const query = useAsyncData(getPromotionRules);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Promotions
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        管理满减、满赠、会员价、等级折扣和充值赠送规则。
      </Typography.Paragraph>

      {query.error ? <Alert type="error" message={query.error} /> : null}

      {query.loading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col span={6}>
              <Card>
                <Typography.Text type="secondary">Active Rules</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  {(query.data ?? []).filter((item) => item.status === "ACTIVE").length}
                </Typography.Title>
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Typography.Text type="secondary">Rule Types</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  5
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
                { title: "Rule Name", dataIndex: "name" },
                { title: "Type", dataIndex: "type" },
                { title: "Summary", dataIndex: "ruleSummary" },
                { title: "Priority", dataIndex: "priority" },
                { title: "Status", dataIndex: "status" }
              ]}
            />
          </Card>
        </>
      )}
    </div>
  );
}
