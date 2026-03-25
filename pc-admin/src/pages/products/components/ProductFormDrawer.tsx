import { Button, Drawer, Form, Input, InputNumber, Select, Space, Switch, message } from "antd";
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
              price: sku.price.replace("CNY ", ""),
              status: sku.status,
              available: sku.available
            }))
          : [
              {
                name: product?.name,
                price: product?.price?.replace("CNY ", ""),
                status: product?.status ?? "Enabled",
                available: true
              }
            ]
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
        <Form.Item label="Status" name="status">
          <Select
            options={[
              { label: "Enabled", value: "Enabled" },
              { label: "Disabled", value: "Disabled" }
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
                    label="Price (yuan)"
                    name={[field.name, "price"]}
                    rules={[{ required: true }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item label="Status" name={[field.name, "status"]} initialValue="Enabled">
                    <Select
                      options={[
                        { label: "Enabled", value: "Enabled" },
                        { label: "Disabled", value: "Disabled" }
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
}
