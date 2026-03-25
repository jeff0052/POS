import { Alert, Button, Card, Input, Space, Table, Tag, Typography } from "antd";
import { useMemo, useState } from "react";
import { getCategories } from "../../api/services/categoryService";
import { createProduct, getProducts, updateProduct } from "../../api/services/productService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { Product } from "../../types";
import { ProductFormDrawer, type ProductFormValues } from "./components/ProductFormDrawer";

const columns = [
  { title: "Name", dataIndex: "name", key: "name" },
  { title: "Category", dataIndex: "categoryName", key: "categoryName" },
  { title: "Barcode", dataIndex: "barcode", key: "barcode" },
  { title: "Price", dataIndex: "price", key: "price" },
  { title: "Stock", dataIndex: "stock", key: "stock" },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: string) => <Tag color={value === "Enabled" ? "green" : "default"}>{value}</Tag>
  }
];

export function ProductsPage() {
  const [keyword, setKeyword] = useState("");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const query = useAsyncData(getProducts);
  const categoriesQuery = useAsyncData(getCategories);
  const [products, setProducts] = useState<Product[]>([]);
  const data = useMemo(() => {
    const list = products.length > 0 ? products : query.data ?? [];
    if (!keyword.trim()) return list;
    return list.filter((item) => item.name.toLowerCase().includes(keyword.toLowerCase()));
  }, [products, query.data, keyword]);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Products
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        管理商品、价格、条码和库存的第一版页面。
      </Typography.Paragraph>

      <Card>
        <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
          <Space>
            <Input
              placeholder="Search product"
              style={{ width: 240 }}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <Button
              onClick={() => {
                setSelectedProduct(null);
                setDrawerOpen(true);
              }}
            >
              Add Product
            </Button>
          </Space>
          <Button type="primary">Import Later</Button>
        </Space>
        {query.error ? <Alert type="error" message={query.error} style={{ marginTop: 16 }} /> : null}

        <Table
          style={{ marginTop: 16 }}
          rowKey="name"
          columns={columns}
          loading={query.loading}
          dataSource={data}
          locale={{ emptyText: "No products yet" }}
          onRow={(record) => ({
            onClick: () => {
              setSelectedProduct(record);
              setDrawerOpen(true);
            }
          })}
        />
      </Card>
      <ProductFormDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        product={selectedProduct}
        categories={categoriesQuery.data ?? []}
        onSubmit={async (values: ProductFormValues) => {
          const current = products.length > 0 ? products : query.data ?? [];
          const payload = {
            storeCode: "1001",
            categoryId: values.categoryId ?? 0,
            name: values.name,
            barcode: values.barcode,
            status: values.status === "Enabled" ? "ENABLED" as const : "DISABLED" as const,
            skus: (values.skus ?? []).map((sku) => ({
              skuId: sku.skuId,
              skuCode: sku.skuCode,
              name: sku.name,
              priceCents: Math.round(Number(sku.price ?? "0") * 100),
              status: sku.status === "Enabled" ? "ENABLED" as const : "DISABLED" as const,
              available: sku.available
            }))
          };
          const saved = selectedProduct
            ? await updateProduct(selectedProduct.id, payload)
            : await createProduct(payload);

          if (selectedProduct) {
            setProducts(current.map((item) => (item.id === selectedProduct.id ? saved : item)));
          } else {
            setProducts([saved, ...current]);
          }
        }}
      />
    </div>
  );
}
