import { useCallback, useMemo, useState } from "react";
import { Alert, Button, Card, Col, Form, Input, InputNumber, message, Row, Select, Skeleton, Table, Typography } from "antd";
import { getProducts } from "../../api/services/productService";
import {
  createPromotionRule,
  getPromotionRuleDetail,
  getPromotionRules,
  updatePromotionRule
} from "../../api/services/promotionService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function PromotionsPage() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [selectedRuleId, setSelectedRuleId] = useState<number | null>(null);
  const [ruleLoading, setRuleLoading] = useState(false);
  const [messageApi, messageContextHolder] = message.useMessage();
  const [form] = Form.useForm();

  const rulesLoader = useCallback(() => getPromotionRules(), [refreshKey]);
  const query = useAsyncData(rulesLoader);
  const productsQuery = useAsyncData(getProducts);

  const selectedRule = useMemo(
    () => (query.data ?? []).find((item) => item.id === selectedRuleId) ?? null,
    [query.data, selectedRuleId]
  );

  async function loadRule(ruleId: number | null) {
    setSelectedRuleId(ruleId);
    if (!ruleId) {
      form.resetFields();
      form.setFieldsValue({
        ruleType: "FULL_REDUCTION",
        ruleStatus: "ACTIVE",
        priority: 100,
        thresholdAmount: 30,
        discountAmount: 3,
        giftQuantity: 1
      });
      return;
    }

    const detail = await getPromotionRuleDetail(ruleId);
    form.setFieldsValue({
      ruleCode: detail.ruleCode,
      ruleName: detail.ruleName,
      ruleType: detail.ruleType,
      ruleStatus: detail.ruleStatus,
      priority: detail.priority,
      thresholdAmount: (detail.thresholdAmountCents ?? 0) / 100,
      discountAmount: detail.discountAmountCents ? detail.discountAmountCents / 100 : undefined,
      giftSkuId: detail.giftSkuId ?? undefined,
      giftQuantity: detail.giftQuantity ?? 1
    });
  }

  async function handleSubmit(values: {
    ruleCode: string;
    ruleName: string;
    ruleType: "FULL_REDUCTION" | "GIFT_SKU";
    ruleStatus: "ACTIVE" | "INACTIVE";
    priority: number;
    thresholdAmount: number;
    discountAmount?: number;
    giftSkuId?: number;
    giftQuantity?: number;
  }) {
    setRuleLoading(true);
    try {
      const payload = {
        merchantId: 1,
        storeId: 101,
        ruleCode: values.ruleCode,
        ruleName: values.ruleName,
        ruleType: values.ruleType,
        ruleStatus: values.ruleStatus,
        priority: values.priority,
        conditionType: "MIN_SUBTOTAL" as const,
        thresholdAmountCents: Math.round(values.thresholdAmount * 100),
        rewardType: values.ruleType === "GIFT_SKU" ? ("GIFT_SKU" as const) : ("DISCOUNT_AMOUNT" as const),
        discountAmountCents: values.ruleType === "FULL_REDUCTION" ? Math.round((values.discountAmount ?? 0) * 100) : undefined,
        giftSkuId: values.ruleType === "GIFT_SKU" ? values.giftSkuId : undefined,
        giftQuantity: values.ruleType === "GIFT_SKU" ? values.giftQuantity : undefined
      };

      if (selectedRuleId) {
        await updatePromotionRule(selectedRuleId, payload);
        messageApi.success("Promotion rule updated.");
      } else {
        await createPromotionRule(payload);
        messageApi.success("Promotion rule created.");
      }

      setRefreshKey((value) => value + 1);
      await loadRule(null);
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "Promotion save failed");
    } finally {
      setRuleLoading(false);
    }
  }

  return (
    <div className="page-shell">
      {messageContextHolder}
      <Typography.Title className="page-title" level={2}>
        Promotions
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        Manage full-reduction, gift-with-purchase, member pricing, tier discounts, and recharge bonus rules.
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
                  2
                </Typography.Title>
              </Card>
            </Col>
          </Row>

          <Card style={{ marginTop: 24 }}>
            <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
              <Col span={8}>
                <Typography.Text type="secondary">Edit Existing Rule</Typography.Text>
                <Select
                  allowClear
                  style={{ width: "100%", marginTop: 12 }}
                  placeholder="Select promotion rule"
                  value={selectedRuleId ?? undefined}
                  options={(query.data ?? []).map((item) => ({
                    value: item.id,
                    label: `${item.name} · ${item.ruleSummary}`
                  }))}
                  onChange={(value) => void loadRule(value ?? null)}
                />
              </Col>
              <Col span={16}>
                <Card size="small">
                  <Typography.Text type="secondary">
                    {selectedRule
                      ? `Editing ${selectedRule.name}`
                      : "Create a new rule. This MVP supports full reduction and gift SKU promotions."}
                  </Typography.Text>
                </Card>
              </Col>
            </Row>

            <Form
              form={form}
              layout="vertical"
              initialValues={{
                ruleType: "FULL_REDUCTION",
                ruleStatus: "ACTIVE",
                priority: 100,
                thresholdAmount: 30,
                discountAmount: 3,
                giftQuantity: 1
              }}
              onFinish={handleSubmit}
            >
              <Row gutter={[16, 0]}>
                <Col span={8}>
                  <Form.Item label="Rule Code" name="ruleCode" rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item label="Rule Name" name="ruleName" rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label="Type" name="ruleType" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { value: "FULL_REDUCTION", label: "Full Reduction" },
                        { value: "GIFT_SKU", label: "Gift SKU" }
                      ]}
                    />
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label="Status" name="ruleStatus" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { value: "ACTIVE", label: "Active" },
                        { value: "INACTIVE", label: "Inactive" }
                      ]}
                    />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item label="Priority" name="priority" rules={[{ required: true }]}>
                    <InputNumber min={1} style={{ width: "100%" }} />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item label="Threshold Amount" name="thresholdAmount" rules={[{ required: true }]}>
                    <InputNumber min={0.01} precision={2} style={{ width: "100%" }} addonBefore="SGD" />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item noStyle shouldUpdate={(prev, next) => prev.ruleType !== next.ruleType}>
                    {({ getFieldValue }) =>
                      getFieldValue("ruleType") === "FULL_REDUCTION" ? (
                        <Form.Item label="Discount Amount" name="discountAmount" rules={[{ required: true }]}>
                          <InputNumber min={0.01} precision={2} style={{ width: "100%" }} addonBefore="SGD" />
                        </Form.Item>
                      ) : (
                        <Form.Item label="Gift SKU" name="giftSkuId" rules={[{ required: true }]}>
                          <Select
                            loading={productsQuery.loading}
                            options={(productsQuery.data ?? []).map((product) => ({
                              value: product.id,
                              label: `${product.name} · ${product.price}`
                            }))}
                          />
                        </Form.Item>
                      )
                    }
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item noStyle shouldUpdate={(prev, next) => prev.ruleType !== next.ruleType}>
                    {({ getFieldValue }) =>
                      getFieldValue("ruleType") === "GIFT_SKU" ? (
                        <Form.Item label="Gift Quantity" name="giftQuantity" rules={[{ required: true }]}>
                          <InputNumber min={1} style={{ width: "100%" }} />
                        </Form.Item>
                      ) : (
                        <Form.Item label=" " colon={false}>
                          <div />
                        </Form.Item>
                      )
                    }
                  </Form.Item>
                </Col>
              </Row>

              <Button type="primary" htmlType="submit" loading={ruleLoading}>
                {selectedRuleId ? "Update Promotion Rule" : "Create Promotion Rule"}
              </Button>
              <Button style={{ marginLeft: 12 }} onClick={() => void loadRule(null)}>
                New Rule
              </Button>
            </Form>

            <Table
              style={{ marginTop: 24 }}
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
