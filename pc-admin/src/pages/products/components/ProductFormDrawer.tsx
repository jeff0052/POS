import { Button, Divider, Drawer, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from "antd";
import { useEffect } from "react";
import type { Category, Product } from "../../../types";

export function ProductFormDrawer({
  open,
  onClose,
  product,
  categories,
  onSubmit
}: {
  open: boolean;
  onClose: () => void;
  product: Product | null;
  categories: Category[];
  onSubmit: (values: ProductFormValues) => Promise<void>;
}) {
  const [form] = Form.useForm<ProductFormValues>();

  useEffect(() => {
    form.setFieldsValue({
      name: product?.name,
      barcode: product?.barcode,
      categoryId: product?.categoryId,
      stock: product?.stock,
      status: product?.status ?? "Enabled",
      skus:
        product?.skus?.length
          ? product.skus.map((sku) => ({
              skuId: sku.id,
              skuCode: sku.code,
              name: sku.name,
              price: sku.price.replace("SGD ", ""),
              status: sku.status,
              available: sku.available
            }))
          : [
              {
                name: product?.name,
                price: product?.price?.replace("SGD ", ""),
                status: product?.status ?? "Enabled",
                available: true
              }
            ],
      attributeGroups: product?.attributeGroups ?? [],
      modifierGroups: product?.modifierGroups ?? [],
      comboSlots:
        product?.comboSlots?.map((slot) => ({
          ...slot,
          allowedSkuCodesText: (slot.allowedSkuCodes ?? []).join(", ")
        })) ?? []
    });
  }, [form, product, open]);

  return (
    <Drawer
      title={product ? `Edit Product: ${product.name}` : "Add Product"}
      open={open}
      onClose={onClose}
      width={520}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={async (values) => {
          await onSubmit(values);
          message.success(product ? "Product saved" : "Product created");
          onClose();
        }}
      >
        <Form.Item label="Product Name" name="name" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Barcode" name="barcode">
          <Input />
        </Form.Item>
        <Form.Item label="Category" name="categoryId" rules={[{ required: true }]}>
          <Select options={categories.map((item) => ({ label: item.name, value: item.id }))} />
        </Form.Item>
        <Form.Item label="Stock" name="stock">
          <InputNumber style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="Shelf Status" name="status">
          <Select
            options={[
              { label: "Published", value: "Enabled" },
              { label: "Unpublished", value: "Disabled" }
            ]}
          />
        </Form.Item>

        <Form.List name="skus">
          {(fields, { add, remove }) => (
            <Space direction="vertical" style={{ width: "100%" }} size={16}>
              {fields.map((field, index) => (
                <div
                  key={field.key}
                  style={{ border: "1px solid #f0f0f0", borderRadius: 12, padding: 16 }}
                >
                  <Space style={{ width: "100%", justifyContent: "space-between" }}>
                    <strong>SKU {index + 1}</strong>
                    {fields.length > 1 ? <Button onClick={() => remove(field.name)}>Remove</Button> : null}
                  </Space>
                  <Form.Item name={[field.name, "skuId"]} hidden>
                    <Input />
                  </Form.Item>
                  <Form.Item label="SKU Name" name={[field.name, "name"]} rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="SKU Code" name={[field.name, "skuCode"]}>
                    <Input />
                  </Form.Item>
                  <Form.Item
                    label="Price (SGD)"
                    name={[field.name, "price"]}
                    rules={[{ required: true }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item label="Shelf Status" name={[field.name, "status"]} initialValue="Enabled">
                    <Select
                      options={[
                        { label: "Published", value: "Enabled" },
                        { label: "Unpublished", value: "Disabled" }
                      ]}
                    />
                  </Form.Item>
                  <Form.Item
                    label="Available in store"
                    name={[field.name, "available"]}
                    valuePropName="checked"
                    initialValue
                  >
                    <Switch />
                  </Form.Item>
                </div>
              ))}
              <Button onClick={() => add({ status: "Enabled", available: true })}>Add SKU</Button>
            </Space>
          )}
        </Form.List>

        <Divider />
        <Typography.Title level={5}>Attribute Groups</Typography.Title>
        <Typography.Paragraph type="secondary">
          Examples include spice level, sugar level, and portion size. Saved at product level for now.
        </Typography.Paragraph>
        <Form.List name="attributeGroups">
          {(fields, { add, remove }) => (
            <Space direction="vertical" style={{ width: "100%" }} size={16}>
              {fields.map((field, index) => (
                <div key={field.key} style={{ border: "1px solid #f0f0f0", borderRadius: 12, padding: 16 }}>
                  <Space style={{ width: "100%", justifyContent: "space-between" }}>
                    <strong>Attribute Group {index + 1}</strong>
                    <Button onClick={() => remove(field.name)}>Remove</Button>
                  </Space>
                  <Form.Item label="Group Name" name={[field.name, "name"]} rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Group Code" name={[field.name, "code"]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Selection Mode" name={[field.name, "selectionMode"]} initialValue="SINGLE">
                    <Select
                      options={[
                        { label: "Single", value: "SINGLE" },
                        { label: "Multiple", value: "MULTIPLE" }
                      ]}
                    />
                  </Form.Item>
                  <Form.Item label="Required" name={[field.name, "required"]} valuePropName="checked" initialValue={false}>
                    <Switch />
                  </Form.Item>
                  <Space style={{ width: "100%" }}>
                    <Form.Item label="Min Select" name={[field.name, "minSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                    <Form.Item label="Max Select" name={[field.name, "maxSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                  </Space>
                  <Form.List name={[field.name, "values"]}>
                    {(valueFields, valueOps) => (
                      <Space direction="vertical" style={{ width: "100%" }}>
                        {valueFields.map((valueField, valueIndex) => (
                          <div
                            key={valueField.key}
                            style={{ border: "1px dashed #d9d9d9", borderRadius: 12, padding: 12 }}
                          >
                            <Space style={{ width: "100%", justifyContent: "space-between" }}>
                              <strong>Option {valueIndex + 1}</strong>
                              <Button onClick={() => valueOps.remove(valueField.name)}>Remove</Button>
                            </Space>
                            <Form.Item label="Label" name={[valueField.name, "label"]} rules={[{ required: true }]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Code" name={[valueField.name, "code"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Price Delta (yuan)" name={[valueField.name, "priceDelta"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Kitchen Label" name={[valueField.name, "kitchenLabel"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item
                              label="Default Selected"
                              name={[valueField.name, "defaultSelected"]}
                              valuePropName="checked"
                              initialValue={false}
                            >
                              <Switch />
                            </Form.Item>
                          </div>
                        ))}
                        <Button onClick={() => valueOps.add({ defaultSelected: false })}>Add Option</Button>
                      </Space>
                    )}
                  </Form.List>
                </div>
              ))}
              <Button
                onClick={() =>
                  add({
                    selectionMode: "SINGLE",
                    required: false,
                    values: [{ defaultSelected: false }]
                  })
                }
              >
                Add Attribute Group
              </Button>
            </Space>
          )}
        </Form.List>

        <Divider />
        <Typography.Title level={5}>Modifier Groups</Typography.Title>
        <Typography.Paragraph type="secondary">
          Examples include add-ons, removals, and extra sides. Saved as product configuration for now.
        </Typography.Paragraph>
        <Form.List name="modifierGroups">
          {(fields, { add, remove }) => (
            <Space direction="vertical" style={{ width: "100%" }} size={16}>
              {fields.map((field, index) => (
                <div key={field.key} style={{ border: "1px solid #f0f0f0", borderRadius: 12, padding: 16 }}>
                  <Space style={{ width: "100%", justifyContent: "space-between" }}>
                    <strong>Modifier Group {index + 1}</strong>
                    <Button onClick={() => remove(field.name)}>Remove</Button>
                  </Space>
                  <Form.Item label="Group Name" name={[field.name, "name"]} rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Group Code" name={[field.name, "code"]}>
                    <Input />
                  </Form.Item>
                  <Space style={{ width: "100%" }}>
                    <Form.Item label="Free Quantity" name={[field.name, "freeQuantity"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                    <Form.Item label="Min Select" name={[field.name, "minSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                    <Form.Item label="Max Select" name={[field.name, "maxSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                  </Space>
                  <Form.List name={[field.name, "options"]}>
                    {(optionFields, optionOps) => (
                      <Space direction="vertical" style={{ width: "100%" }}>
                        {optionFields.map((optionField, optionIndex) => (
                          <div
                            key={optionField.key}
                            style={{ border: "1px dashed #d9d9d9", borderRadius: 12, padding: 12 }}
                          >
                            <Space style={{ width: "100%", justifyContent: "space-between" }}>
                              <strong>Modifier {optionIndex + 1}</strong>
                              <Button onClick={() => optionOps.remove(optionField.name)}>Remove</Button>
                            </Space>
                            <Form.Item label="Label" name={[optionField.name, "label"]} rules={[{ required: true }]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Code" name={[optionField.name, "code"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Price Delta (yuan)" name={[optionField.name, "priceDelta"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item label="Kitchen Label" name={[optionField.name, "kitchenLabel"]}>
                              <Input />
                            </Form.Item>
                            <Form.Item
                              label="Default Selected"
                              name={[optionField.name, "defaultSelected"]}
                              valuePropName="checked"
                              initialValue={false}
                            >
                              <Switch />
                            </Form.Item>
                          </div>
                        ))}
                        <Button onClick={() => optionOps.add({ defaultSelected: false })}>Add Modifier Option</Button>
                      </Space>
                    )}
                  </Form.List>
                </div>
              ))}
              <Button onClick={() => add({ options: [{ defaultSelected: false }] })}>Add Modifier Group</Button>
            </Space>
          )}
        </Form.List>

        <Divider />
        <Typography.Title level={5}>Combo Slots</Typography.Title>
        <Typography.Paragraph type="secondary">
          Combo slots are currently configured with allowed SKU codes. A richer selector can be added later.
        </Typography.Paragraph>
        <Form.List name="comboSlots">
          {(fields, { add, remove }) => (
            <Space direction="vertical" style={{ width: "100%" }} size={16}>
              {fields.map((field, index) => (
                <div key={field.key} style={{ border: "1px solid #f0f0f0", borderRadius: 12, padding: 16 }}>
                  <Space style={{ width: "100%", justifyContent: "space-between" }}>
                    <strong>Combo Slot {index + 1}</strong>
                    <Button onClick={() => remove(field.name)}>Remove</Button>
                  </Space>
                  <Form.Item label="Slot Name" name={[field.name, "name"]} rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Slot Code" name={[field.name, "code"]}>
                    <Input />
                  </Form.Item>
                  <Space style={{ width: "100%" }}>
                    <Form.Item label="Min Select" name={[field.name, "minSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                    <Form.Item label="Max Select" name={[field.name, "maxSelect"]}>
                      <InputNumber style={{ width: "100%" }} />
                    </Form.Item>
                  </Space>
                  <Form.Item
                    label="Allowed SKU Codes"
                    name={[field.name, "allowedSkuCodesText"]}
                    extra="Use comma-separated sku codes, for example: noodle-small, noodle-large"
                  >
                    <Input.TextArea rows={3} />
                  </Form.Item>
                </div>
              ))}
              <Button onClick={() => add({})}>Add Combo Slot</Button>
            </Space>
          )}
        </Form.List>

        <Space style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit">
            Save
          </Button>
          <Button onClick={onClose}>Cancel</Button>
        </Space>
      </Form>
    </Drawer>
  );
}

export interface ProductFormValues {
  name: string;
  barcode?: string;
  categoryId?: number;
  stock?: number;
  status: "Enabled" | "Disabled";
  skus: Array<{
    skuId?: number;
    skuCode?: string;
    name: string;
    price?: string;
    status: "Enabled" | "Disabled";
    available: boolean;
  }>;
  attributeGroups?: Array<{
    code?: string;
    name: string;
    selectionMode: "SINGLE" | "MULTIPLE";
    required: boolean;
    minSelect?: number;
    maxSelect?: number;
    values?: Array<{
      code?: string;
      label: string;
      priceDelta?: string;
      defaultSelected: boolean;
      kitchenLabel?: string;
    }>;
  }>;
  modifierGroups?: Array<{
    code?: string;
    name: string;
    freeQuantity?: number;
    minSelect?: number;
    maxSelect?: number;
    options?: Array<{
      code?: string;
      label: string;
      priceDelta?: string;
      defaultSelected: boolean;
      kitchenLabel?: string;
    }>;
  }>;
  comboSlots?: Array<{
    code?: string;
    name: string;
    minSelect?: number;
    maxSelect?: number;
    allowedSkuCodesText?: string;
  }>;
}
