import { Alert, Button, Card, Input, Space, Table, Typography } from "antd";
import { useMemo, useState } from "react";
import { createCategory, getCategories, updateCategory } from "../../api/services/categoryService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { Category } from "../../types";
import { CategoryFormModal, type CategoryFormValues } from "./components/CategoryFormModal";

const columns = [
  { title: "Category", dataIndex: "name", key: "name" },
  { title: "Sort", dataIndex: "sortOrder", key: "sortOrder" },
  { title: "Status", dataIndex: "status", key: "status" }
];

export function CategoriesPage() {
  const [keyword, setKeyword] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);
  const query = useAsyncData(getCategories);
  const [categories, setCategories] = useState<Category[]>([]);
  const data = useMemo(() => {
    const list = categories.length > 0 ? categories : query.data ?? [];
    if (!keyword.trim()) return list;
    return list.filter((item) => item.name.toLowerCase().includes(keyword.toLowerCase()));
  }, [categories, query.data, keyword]);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Categories
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        商品分类管理页，给收银端分类 tab 和后台商品组织使用。
      </Typography.Paragraph>

      <Card>
        <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
          <Input
            placeholder="Search category"
            style={{ width: 240 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <Button
            type="primary"
            onClick={() => {
              setSelectedCategory(null);
              setModalOpen(true);
            }}
          >
            Add Category
          </Button>
        </Space>
        {query.error ? <Alert type="error" message={query.error} style={{ marginTop: 16 }} /> : null}
        <Table
          style={{ marginTop: 16 }}
          rowKey="name"
          columns={columns}
          loading={query.loading}
          dataSource={data}
          locale={{ emptyText: "No categories yet" }}
          onRow={(record) => ({
            onClick: () => {
              setSelectedCategory(record);
              setModalOpen(true);
            }
          })}
        />
      </Card>
      <CategoryFormModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        category={selectedCategory}
        onSubmit={async (values: CategoryFormValues) => {
          const current = categories.length > 0 ? categories : query.data ?? [];
          const payload = {
            storeCode: "1001",
            name: values.name,
            sortOrder: values.sortOrder ?? 0,
            enabled: values.status === "Enabled"
          };
          const saved = selectedCategory
            ? await updateCategory(selectedCategory.id, payload)
            : await createCategory(payload);

          if (selectedCategory) {
            setCategories(current.map((item) => (item.id === selectedCategory.id ? saved : item)));
          } else {
            setCategories([saved, ...current]);
          }
        }}
      />
    </div>
  );
}
