import { Button, Form, Input, InputNumber, Modal, Select, Space, message } from "antd";
import { useEffect } from "react";
import type { Category } from "../../../types";

export function CategoryFormModal({
  open,
  onClose,
  category,
  onSubmit
}: {
  open: boolean;
  onClose: () => void;
  category: Category | null;
  onSubmit: (values: CategoryFormValues) => Promise<void>;
}) {
  const [form] = Form.useForm<CategoryFormValues>();

  useEffect(() => {
    form.setFieldsValue({
      name: category?.name,
      sortOrder: category?.sortOrder,
      status: category?.status ?? "Enabled"
    });
  }, [form, category, open]);

  return (
    <Modal
      title={category ? `Edit Category: ${category.name}` : "Add Category"}
      open={open}
      onCancel={onClose}
      footer={null}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={async (values) => {
          await onSubmit(values);
          message.success(category ? "Category draft updated" : "Category draft created");
          onClose();
        }}
      >
        <Form.Item label="Category Name" name="name" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Sort Order" name="sortOrder">
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
          <Button
            type="primary"
            onClick={() => {
              form.submit();
            }}
          >
            Save
          </Button>
          <Button onClick={onClose}>Cancel</Button>
        </Space>
      </Form>
    </Modal>
  );
}

export interface CategoryFormValues {
  name: string;
  sortOrder?: number;
  status: "Enabled" | "Disabled";
}
