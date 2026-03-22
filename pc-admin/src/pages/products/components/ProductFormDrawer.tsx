import { Button, Drawer, Form, Input, InputNumber, Select, Space, message } from "antd";
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
  onSubmit: (values: ProductFormValues) => void;
}) {
  const [form] = Form.useForm<ProductFormValues>();

  useEffect(() => {
    form.setFieldsValue({
      name: product?.name,
      barcode: product?.barcode,
      categoryName: product?.categoryName,
      price: product?.price?.replace("CNY ", ""),
      stock: product?.stock,
      status: product?.status ?? "Enabled"
    });
  }, [form, product, open]);

  return (
    <Drawer
      title={product ? `Edit Product: ${product.name}` : "Add Product"}
      open={open}
      onClose={onClose}
      width={460}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => {
          onSubmit(values);
          message.success(product ? "Product draft updated" : "Product draft created");
          onClose();
        }}
      >
        <Form.Item label="Product Name" name="name" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Barcode" name="barcode">
          <Input />
        </Form.Item>
        <Form.Item label="Category" name="categoryName">
          <Select options={categories.map((item) => ({ label: item.name, value: item.name }))} />
        </Form.Item>
        <Form.Item label="Price (yuan)" name="price">
          <Input />
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
        <Space>
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
  categoryName?: string;
  price?: string;
  stock?: number;
  status: "Enabled" | "Disabled";
}
