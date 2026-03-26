import { useCallback, useMemo, useState } from "react";
import { Alert, Button, Card, Col, Descriptions, Form, Input, InputNumber, message, Row, Select, Skeleton, Table, Tabs, Typography } from "antd";
import {
  adjustMemberPoints,
  createMember,
  getMemberDetail,
  getMembers,
  getMemberTiers,
  getPointsRecords,
  getRechargeRecords,
  rechargeMember,
  updateMember
} from "../../api/services/memberService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function CrmPage() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [messageApi, messageContextHolder] = message.useMessage();
  const [createForm] = Form.useForm();
  const [detailForm] = Form.useForm();
  const [rechargeForm] = Form.useForm();
  const [pointsForm] = Form.useForm();

  const membersLoader = useCallback(() => getMembers(), [refreshKey]);
  const rechargeLoader = useCallback(() => getRechargeRecords(), [refreshKey]);
  const pointsLoader = useCallback(() => getPointsRecords(), [refreshKey]);

  const membersQuery = useAsyncData(membersLoader);
  const tiersQuery = useAsyncData(getMemberTiers);
  const rechargeQuery = useAsyncData(rechargeLoader);
  const pointsQuery = useAsyncData(pointsLoader);
  const memberDetailQuery = useAsyncData(
    useCallback(() => {
      if (!selectedMemberId) {
        return Promise.resolve(null);
      }
      return getMemberDetail(selectedMemberId);
    }, [selectedMemberId, refreshKey])
  );

  const selectedMember = useMemo(
    () => membersQuery.data?.find((item) => item.id === selectedMemberId) ?? null,
    [membersQuery.data, selectedMemberId]
  );

  const selectedMemberDetail = memberDetailQuery.data;

  const summary = membersQuery.data
    ? [
        { title: "Active Members", value: String(membersQuery.data.filter((item) => item.status === "ACTIVE").length) },
        {
          title: "Member Balance",
          value: `SGD ${membersQuery.data
            .reduce((total, item) => total + Number(item.balance.replace("SGD ", "")), 0)
            .toFixed(2)}`
        },
        {
          title: "Today Recharge",
          value: `SGD ${rechargeQuery.data
            ?.reduce((total, item) => total + Number(item.amount.replace("SGD ", "")), 0)
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

  async function handleCreateMember(values: { name: string; phone: string; tierCode?: string }) {
    setActionLoading(true);
    try {
      const created = await createMember(values.name, values.phone, values.tierCode);
      messageApi.success("Member created.");
      createForm.resetFields();
      setSelectedMemberId(created.id);
      setRefreshKey((value) => value + 1);
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "Create member failed");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleUpdateMember(values: {
    name: string;
    phone: string;
    tierCode: string;
    memberStatus: "ACTIVE" | "INACTIVE";
  }) {
    if (!selectedMemberId) {
      messageApi.warning("Select a member first.");
      return;
    }

    setActionLoading(true);
    try {
      await updateMember(selectedMemberId, values);
      messageApi.success("Member updated.");
      setRefreshKey((value) => value + 1);
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "Update member failed");
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
        Operating center for members, points, recharges, tiers, and benefits.
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
            <Row gutter={[16, 16]}>
              <Col span={10}>
                <Typography.Text type="secondary">Create Member</Typography.Text>
                <Typography.Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
                  Add a new member before recharge, points adjustment, or POS binding.
                </Typography.Paragraph>
              </Col>
              <Col span={14}>
                <Form form={createForm} layout="vertical" onFinish={handleCreateMember}>
                  <Row gutter={[12, 12]}>
                    <Col span={8}>
                      <Form.Item label="Name" name="name" rules={[{ required: true }]}>
                        <Input placeholder="Member name" />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item label="Phone" name="phone" rules={[{ required: true }]}>
                        <Input placeholder="Phone number" />
                      </Form.Item>
                    </Col>
                    <Col span={4}>
                      <Form.Item label="Tier" name="tierCode" initialValue="STANDARD">
                        <Select
                          options={(tiersQuery.data ?? []).map((tier) => ({
                            value: tier.name.toUpperCase(),
                            label: tier.name
                          }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={4} style={{ display: "flex", alignItems: "end" }}>
                      <Button block type="primary" htmlType="submit" loading={actionLoading}>
                        Create
                      </Button>
                    </Col>
                  </Row>
                </Form>
              </Col>
            </Row>
          </Card>

          <Card style={{ marginTop: 24 }}>
            <Row gutter={[16, 16]} align="middle">
              <Col span={7}>
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
              <Col span={9}>
                <Typography.Text type="secondary">Member Detail</Typography.Text>
                <Card size="small" style={{ marginTop: 12 }}>
                  {memberDetailQuery.loading && selectedMemberId ? (
                    <Skeleton active paragraph={{ rows: 4 }} />
                  ) : selectedMemberDetail ? (
                    <>
                      <Descriptions column={1} size="small">
                        <Descriptions.Item label="Member No">{selectedMemberDetail.memberNo}</Descriptions.Item>
                        <Descriptions.Item label="Tier">{selectedMemberDetail.tierName}</Descriptions.Item>
                        <Descriptions.Item label="Status">{selectedMemberDetail.status}</Descriptions.Item>
                        <Descriptions.Item label="Balance">{selectedMemberDetail.balance}</Descriptions.Item>
                        <Descriptions.Item label="Total Spent">{selectedMemberDetail.totalSpent}</Descriptions.Item>
                        <Descriptions.Item label="Total Recharge">{selectedMemberDetail.totalRecharge}</Descriptions.Item>
                      </Descriptions>
                      <Form
                        key={selectedMemberDetail.id}
                        form={detailForm}
                        layout="vertical"
                        initialValues={{
                          name: selectedMemberDetail.name,
                          phone: selectedMemberDetail.phone,
                          tierCode: selectedMemberDetail.tierName,
                          memberStatus: selectedMemberDetail.status
                        }}
                        onFinish={handleUpdateMember}
                      >
                        <Row gutter={[8, 8]}>
                          <Col span={12}>
                            <Form.Item label="Name" name="name" rules={[{ required: true }]}>
                              <Input />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="Phone" name="phone" rules={[{ required: true }]}>
                              <Input />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="Tier" name="tierCode" rules={[{ required: true }]}>
                              <Select
                                options={(tiersQuery.data ?? []).map((tier) => ({
                                  value: tier.name.toUpperCase(),
                                  label: tier.name
                                }))}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={12}>
                            <Form.Item label="Status" name="memberStatus" rules={[{ required: true }]}>
                              <Select
                                options={[
                                  { value: "ACTIVE", label: "ACTIVE" },
                                  { value: "INACTIVE", label: "INACTIVE" }
                                ]}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        <Button block htmlType="submit" loading={actionLoading}>
                          Save Member Detail
                        </Button>
                      </Form>
                    </>
                  ) : (
                    <Typography.Paragraph style={{ marginBottom: 0 }}>
                      Choose a member to review and edit member profile.
                    </Typography.Paragraph>
                  )}
                </Card>
              </Col>
              <Col span={4}>
                <Form form={rechargeForm} layout="vertical" onFinish={handleRecharge}>
                  <Form.Item label="Recharge Amount" name="amount" rules={[{ required: true }]}>
                    <InputNumber min={0.01} precision={2} style={{ width: "100%" }} addonBefore="SGD" />
                  </Form.Item>
                  <Form.Item label="Bonus Amount" name="bonusAmount" initialValue={0}>
                    <InputNumber min={0} precision={2} style={{ width: "100%" }} addonBefore="SGD" />
                  </Form.Item>
                  <Button block type="primary" htmlType="submit" loading={actionLoading}>
                    Recharge Member
                  </Button>
                </Form>
              </Col>
              <Col span={4}>
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
                      { title: "Status", dataIndex: "status" },
                      { title: "Points", dataIndex: "points" },
                      { title: "Balance", dataIndex: "balance" },
                      { title: "Total Spent", dataIndex: "totalSpent" },
                      { title: "Total Recharge", dataIndex: "totalRecharge" }
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
