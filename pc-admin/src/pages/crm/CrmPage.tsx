import { useCallback, useMemo, useState } from "react";
import { Alert, Button, Card, Col, Form, InputNumber, message, Row, Select, Skeleton, Space, Table, Tabs, Typography } from "antd";
import {
  adjustMemberPoints,
  getMembers,
  getMemberTiers,
  getPointsRecords,
  getRechargeRecords,
  rechargeMember
} from "../../api/services/memberService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function CrmPage() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [messageApi, messageContextHolder] = message.useMessage();
  const [rechargeForm] = Form.useForm();
  const [pointsForm] = Form.useForm();

  const membersLoader = useCallback(() => getMembers(), [refreshKey]);
  const rechargeLoader = useCallback(() => getRechargeRecords(), [refreshKey]);
  const pointsLoader = useCallback(() => getPointsRecords(), [refreshKey]);

  const membersQuery = useAsyncData(membersLoader);
  const tiersQuery = useAsyncData(getMemberTiers);
  const rechargeQuery = useAsyncData(rechargeLoader);
  const pointsQuery = useAsyncData(pointsLoader);

  const selectedMember = useMemo(
    () => membersQuery.data?.find((item) => item.id === selectedMemberId) ?? null,
    [membersQuery.data, selectedMemberId]
  );

  const summary = membersQuery.data
    ? [
        { title: "Active Members", value: String(membersQuery.data.filter((item) => item.status === "ACTIVE").length) },
        {
          title: "Member Balance",
          value: `CNY ${membersQuery.data
            .reduce((total, item) => total + Number(item.balance.replace("CNY ", "")), 0)
            .toFixed(2)}`
        },
        {
          title: "Today Recharge",
          value: `CNY ${rechargeQuery.data
            ?.reduce((total, item) => total + Number(item.amount.replace("CNY ", "")), 0)
            .toFixed(2) ?? "0.00"}`
        },
        { title: "Points Changes", value: `${pointsQuery.data?.length ?? 0} records` }
      ]
    : [];

  async function handleRecharge(values: { amount: number; bonusAmount: number }) {
    if (!selectedMemberId) {
      messageApi.warning("Select a member first.");
      return;
    }

    setActionLoading(true);
    try {
      await rechargeMember(selectedMemberId, Math.round(values.amount * 100), Math.round(values.bonusAmount * 100), "CRM Operator");
      messageApi.success("Recharge completed.");
      rechargeForm.resetFields();
      setRefreshKey((value) => value + 1);
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "Recharge failed");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleAdjustPoints(values: { pointsDelta: number }) {
    if (!selectedMemberId) {
      messageApi.warning("Select a member first.");
      return;
    }

    setActionLoading(true);
    try {
      await adjustMemberPoints(selectedMemberId, values.pointsDelta, "ADJUST", "MANUAL", "CRM Operator");
      messageApi.success("Points updated.");
      pointsForm.resetFields();
      setRefreshKey((value) => value + 1);
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "Points update failed");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="page-shell">
      {messageContextHolder}
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

          <Card style={{ marginTop: 24 }}>
            <Row gutter={[16, 16]} align="middle">
              <Col span={8}>
                <Typography.Text type="secondary">Member Operations</Typography.Text>
                <Select
                  style={{ width: "100%", marginTop: 12 }}
                  placeholder="Select member"
                  value={selectedMemberId ?? undefined}
                  options={(membersQuery.data ?? []).map((member) => ({
                    value: member.id,
                    label: `${member.name} · ${member.phone}`
                  }))}
                  onChange={(value) => setSelectedMemberId(value)}
                />
                <Typography.Paragraph style={{ marginTop: 12, marginBottom: 0 }}>
                  {selectedMember
                    ? `${selectedMember.name} · ${selectedMember.tierName} · ${selectedMember.balance} · ${selectedMember.points} pts`
                    : "Choose a member to recharge or adjust points."}
                </Typography.Paragraph>
              </Col>
              <Col span={8}>
                <Form form={rechargeForm} layout="vertical" onFinish={handleRecharge}>
                  <Form.Item label="Recharge Amount" name="amount" rules={[{ required: true }]}>
                    <InputNumber min={0.01} precision={2} style={{ width: "100%" }} addonBefore="CNY" />
                  </Form.Item>
                  <Form.Item label="Bonus Amount" name="bonusAmount" initialValue={0}>
                    <InputNumber min={0} precision={2} style={{ width: "100%" }} addonBefore="CNY" />
                  </Form.Item>
                  <Button block type="primary" htmlType="submit" loading={actionLoading}>
                    Recharge Member
                  </Button>
                </Form>
              </Col>
              <Col span={8}>
                <Form form={pointsForm} layout="vertical" onFinish={handleAdjustPoints}>
                  <Form.Item label="Points Adjustment" name="pointsDelta" rules={[{ required: true }]}>
                    <InputNumber style={{ width: "100%" }} />
                  </Form.Item>
                  <Form.Item label="Change Type">
                    <Select disabled value="ADJUST" options={[{ value: "ADJUST", label: "Manual Adjust" }]} />
                  </Form.Item>
                  <Button block htmlType="submit" loading={actionLoading}>
                    Update Points
                  </Button>
                </Form>
              </Col>
            </Row>
          </Card>

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
