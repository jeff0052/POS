import { Alert, Card, Col, Row, Skeleton, Table, Tabs, Typography } from "antd";
import { getMembers, getMemberTiers, getPointsRecords, getRechargeRecords } from "../../api/services/memberService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function CrmPage() {
  const membersQuery = useAsyncData(getMembers);
  const tiersQuery = useAsyncData(getMemberTiers);
  const rechargeQuery = useAsyncData(getRechargeRecords);
  const pointsQuery = useAsyncData(getPointsRecords);

  const summary = membersQuery.data
    ? [
        { title: "Active Members", value: String(membersQuery.data.filter((item) => item.status === "ACTIVE").length) },
        { title: "Member Balance", value: "CNY 408.00" },
        { title: "Today Recharge", value: "CNY 700.00" },
        { title: "Points Changes", value: "2 records" }
      ]
    : [];

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        CRM
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        会员、积分、充值、等级与权益的运营中心。
      </Typography.Paragraph>

      {membersQuery.error ? <Alert type="error" message={membersQuery.error} /> : null}

      {membersQuery.loading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {summary.map((item) => (
              <Col span={6} key={item.title}>
                <Card>
                  <Typography.Text type="secondary">{item.title}</Typography.Text>
                  <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                    {item.value}
                  </Typography.Title>
                </Card>
              </Col>
            ))}
          </Row>

          <Tabs
            style={{ marginTop: 24 }}
            items={[
              {
                key: "members",
                label: "Members",
                children: (
                  <Table
                    rowKey="id"
                    dataSource={membersQuery.data ?? []}
                    pagination={false}
                    columns={[
                      { title: "Name", dataIndex: "name" },
                      { title: "Phone", dataIndex: "phone" },
                      { title: "Tier", dataIndex: "tierName" },
                      { title: "Points", dataIndex: "points" },
                      { title: "Balance", dataIndex: "balance" },
                      { title: "Total Spent", dataIndex: "totalSpent" }
                    ]}
                  />
                )
              },
              {
                key: "tiers",
                label: "Tier Rules",
                children: (
                  <Table
                    rowKey="id"
                    dataSource={tiersQuery.data ?? []}
                    pagination={false}
                    columns={[
                      { title: "Tier", dataIndex: "name" },
                      { title: "Upgrade Rule", dataIndex: "upgradeRule" },
                      {
                        title: "Benefits",
                        dataIndex: "benefits",
                        render: (value: string[]) => value.join(" / ")
                      }
                    ]}
                  />
                )
              },
              {
                key: "recharge",
                label: "Recharge Records",
                children: (
                  <Table
                    rowKey="id"
                    dataSource={rechargeQuery.data ?? []}
                    pagination={false}
                    columns={[
                      { title: "Member", dataIndex: "memberName" },
                      { title: "Phone", dataIndex: "memberPhone" },
                      { title: "Amount", dataIndex: "amount" },
                      { title: "Bonus", dataIndex: "bonusAmount" },
                      { title: "Status", dataIndex: "status" },
                      { title: "Time", dataIndex: "time" }
                    ]}
                  />
                )
              },
              {
                key: "points",
                label: "Points Ledger",
                children: (
                  <Table
                    rowKey="id"
                    dataSource={pointsQuery.data ?? []}
                    pagination={false}
                    columns={[
                      { title: "Member", dataIndex: "memberName" },
                      { title: "Change", dataIndex: "changeType" },
                      { title: "Points", dataIndex: "points" },
                      { title: "Source", dataIndex: "source" },
                      { title: "Time", dataIndex: "time" }
                    ]}
                  />
                )
              }
            ]}
          />
        </>
      )}
    </div>
  );
}
