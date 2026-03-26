import { Alert, Button, Card, Input, Popconfirm, Space, Table, Tag, Typography, message } from "antd";
import { useMemo, useState } from "react";
import { getCategories } from "../../api/services/categoryService";
import { createProduct, getProducts, updateProduct } from "../../api/services/productService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { Product } from "../../types";
import { ProductFormDrawer, type ProductFormValues } from "./components/ProductFormDrawer";

function parseMoney(value: string): number {
  return Math.round(Number(value.replace("SGD ", "").trim() || "0") * 100);
}

function buildProductPayload(product: Product) {
  return {
    storeCode: "1001",
    categoryId: product.categoryId ?? 0,
    name: product.name,
    barcode: product.barcode,
    status: product.status === "Enabled" ? ("ENABLED" as const) : ("DISABLED" as const),
    skus: (product.skus ?? []).map((sku) => ({
      skuId: sku.id,
      skuCode: sku.code,
      name: sku.name,
      priceCents: parseMoney(sku.price),
      status: sku.status === "Enabled" ? ("ENABLED" as const) : ("DISABLED" as const),
      available: sku.available
    })),
    attributeGroups: (product.attributeGroups ?? []).map((group) => ({
      code: group.code,
      name: group.name,
      selectionMode: group.selectionMode,
      required: group.required,
      minSelect: group.minSelect,
      maxSelect: group.maxSelect,
      values: (group.values ?? []).map((value) => ({
        code: value.code,
        label: value.label,
        priceDeltaCents: value.priceDeltaCents,
        defaultSelected: value.defaultSelected,
        kitchenLabel: value.kitchenLabel
      }))
    })),
    modifierGroups: (product.modifierGroups ?? []).map((group) => ({
      code: group.code,
      name: group.name,
      freeQuantity: group.freeQuantity,
      minSelect: group.minSelect,
      maxSelect: group.maxSelect,
      options: (group.options ?? []).map((option) => ({
        code: option.code,
        label: option.label,
        priceDeltaCents: option.priceDeltaCents,
        defaultSelected: option.defaultSelected,
        kitchenLabel: option.kitchenLabel
      }))
    })),
    comboSlots: (product.comboSlots ?? []).map((slot) => ({
      code: slot.code,
      name: slot.name,
      minSelect: slot.minSelect,
      maxSelect: slot.maxSelect,
      allowedSkuCodes: slot.allowedSkuCodes ?? []
    }))
  };
}

const columns = [
  { title: "Name", dataIndex: "name", key: "name" },
  { title: "Category", dataIndex: "categoryName", key: "categoryName" },
  { title: "Barcode", dataIndex: "barcode", key: "barcode" },
  { title: "Price", dataIndex: "price", key: "price" },
  { title: "Stock", dataIndex: "stock", key: "stock" },
  {
    title: "Shelf Status",
    dataIndex: "status",
    key: "status",
    render: (value: string) => (
      <Tag color={value === "Enabled" ? "green" : "default"}>
        {value === "Enabled" ? "Published" : "Unpublished"}
      </Tag>
    )
  }
];

export function ProductsPage() {
  const [keyword, setKeyword] = useState("");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const query = useAsyncData(getProducts);
  const categoriesQuery = useAsyncData(getCategories);
  const [products, setProducts] = useState<Product[]>([]);
  const current = products.length > 0 ? products : query.data ?? [];
  const data = useMemo(() => {
    if (!keyword.trim()) return current;
    return current.filter((item) => item.name.toLowerCase().includes(keyword.toLowerCase()));
  }, [current, keyword]);

  const handleToggleShelfStatus = async (product: Product) => {
    const nextStatus = product.status === "Enabled" ? "Disabled" : "Enabled";
    const saved = await updateProduct(product.id, {
      ...buildProductPayload(product),
      status: nextStatus === "Enabled" ? "ENABLED" : "DISABLED"
    });
    setProducts(current.map((item) => (item.id === product.id ? saved : item)));
    message.success(nextStatus === "Enabled" ? "Product published" : "Product unpublished");
  };

  const tableColumns = [
    ...columns,
    {
      title: "Actions",
      key: "actions",
      render: (_: unknown, record: Product) => (
        <Space>
          <Button
            size="small"
            onClick={(event) => {
              event.stopPropagation();
              setSelectedProduct(record);
              setDrawerOpen(true);
            }}
          >
            Edit
          </Button>
          <Popconfirm
            title={record.status === "Enabled" ? "Unpublish this product?" : "Publish this product?"}
            okText={record.status === "Enabled" ? "Unpublish" : "Publish"}
            cancelText="Cancel"
            onConfirm={() => void handleToggleShelfStatus(record)}
          >
            <Button
              size="small"
              onClick={(event) => {
                event.stopPropagation();
              }}
            >
              {record.status === "Enabled" ? "Unpublish" : "Publish"}
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Products
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        First-pass page for managing products, pricing, barcodes, stock, and shelf status.
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
          rowKey="id"
          columns={tableColumns}
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
          const payload = {
            storeCode: "1001",
            categoryId: values.categoryId ?? 0,
            name: values.name,
            barcode: values.barcode,
            status: values.status === "Enabled" ? ("ENABLED" as const) : ("DISABLED" as const),
            skus: (values.skus ?? []).map((sku) => ({
              skuId: sku.skuId,
              skuCode: sku.skuCode,
              name: sku.name,
              priceCents: Math.round(Number(sku.price ?? "0") * 100),
              status: sku.status === "Enabled" ? ("ENABLED" as const) : ("DISABLED" as const),
              available: sku.available
            })),
            attributeGroups: (values.attributeGroups ?? []).map((group) => ({
              code: group.code,
              name: group.name,
              selectionMode: group.selectionMode,
              required: group.required,
              minSelect: group.minSelect,
              maxSelect: group.maxSelect,
              values: (group.values ?? []).map((value) => ({
                code: value.code,
                label: value.label,
                priceDeltaCents: Math.round(Number(value.priceDelta ?? "0") * 100),
                defaultSelected: value.defaultSelected,
                kitchenLabel: value.kitchenLabel
              }))
            })),
            modifierGroups: (values.modifierGroups ?? []).map((group) => ({
              code: group.code,
              name: group.name,
              freeQuantity: group.freeQuantity,
              minSelect: group.minSelect,
              maxSelect: group.maxSelect,
              options: (group.options ?? []).map((option) => ({
                code: option.code,
                label: option.label,
                priceDeltaCents: Math.round(Number(option.priceDelta ?? "0") * 100),
                defaultSelected: option.defaultSelected,
                kitchenLabel: option.kitchenLabel
              }))
            })),
            comboSlots: (values.comboSlots ?? []).map((slot) => ({
              code: slot.code,
              name: slot.name,
              minSelect: slot.minSelect,
              maxSelect: slot.maxSelect,
              allowedSkuCodes: (slot.allowedSkuCodesText ?? "")
                .split(",")
                .map((item) => item.trim())
                .filter(Boolean)
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
