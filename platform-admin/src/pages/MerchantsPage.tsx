import { Breadcrumb, Button, Card, Modal, Form, Input, Spin, Table, Typography } from "antd";
import { useEffect, useState } from "react";
import { apiGetV2 } from "../api/client";

const { Title, Paragraph } = Typography;

type Level = "merchants" | "brands" | "countries" | "stores";

interface MerchantRow { id: number; merchantCode: string; merchantName: string; status: string; brandCount: number; storeCount: number; }
interface BrandRow { id: number; brandCode: string; brandName: string; status: string; countryCount: number; storeCount: number; }
interface CountryRow { id: number; countryCode: string; countryName: string; currencyCode: string; timezone: string; taxRatePercent: number; status: string; storeCount: number; }

export function MerchantsPage() {
  const [level, setLevel] = useState<Level>("merchants");
  const [merchants, setMerchants] = useState<MerchantRow[]>([]);
  const [brands, setBrands] = useState<BrandRow[]>([]);
  const [countries, setCountries] = useState<CountryRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedMerchant, setSelectedMerchant] = useState<MerchantRow | null>(null);
  const [selectedBrand, setSelectedBrand] = useState<BrandRow | null>(null);

  useEffect(() => { loadMerchants(); }, []);

  const loadMerchants = () => {
    setLoading(true);
    apiGetV2<MerchantRow[]>("/platform/merchants").then(setMerchants).finally(() => setLoading(false));
  };

  const loadBrands = (merchant: MerchantRow) => {
    setSelectedMerchant(merchant);
    setLevel("brands");
    setLoading(true);
    apiGetV2<BrandRow[]>(`/platform/merchants/${merchant.id}/brands`).then(setBrands).finally(() => setLoading(false));
  };

  const loadCountries = (brand: BrandRow) => {
    setSelectedBrand(brand);
    setLevel("countries");
    setLoading(true);
    apiGetV2<CountryRow[]>(`/platform/brands/${brand.id}/countries`).then(setCountries).finally(() => setLoading(false));
  };

  const goBack = (to: Level) => {
    setLevel(to);
    if (to === "merchants") { setSelectedMerchant(null); setSelectedBrand(null); }
    if (to === "brands") { setSelectedBrand(null); }
  };

  const breadcrumb = (
    <Breadcrumb style={{ marginBottom: 16 }}>
      <Breadcrumb.Item><a onClick={() => goBack("merchants")}>Merchants</a></Breadcrumb.Item>
      {selectedMerchant && <Breadcrumb.Item><a onClick={() => goBack("brands")}>{selectedMerchant.merchantName}</a></Breadcrumb.Item>}
      {selectedBrand && <Breadcrumb.Item>{selectedBrand.brandName}</Breadcrumb.Item>}
    </Breadcrumb>
  );

  if (loading) return <Spin size="large" style={{ display: "block", margin: "100px auto" }} />;

  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Merchant Hierarchy</Title>
      <Paragraph className="page-subtitle">Merchant → Brand → Country → Store</Paragraph>
      {breadcrumb}

      {level === "merchants" && (
        <Table dataSource={merchants} rowKey="id" pagination={false}
          onRow={(r) => ({ onClick: () => loadBrands(r), style: { cursor: "pointer" } })}
          columns={[
            { title: "Code", dataIndex: "merchantCode" },
            { title: "Name", dataIndex: "merchantName" },
            { title: "Status", dataIndex: "status" },
            { title: "Brands", dataIndex: "brandCount" },
            { title: "Stores", dataIndex: "storeCount" },
          ]}
        />
      )}

      {level === "brands" && (
        <Table dataSource={brands} rowKey="id" pagination={false}
          onRow={(r) => ({ onClick: () => loadCountries(r), style: { cursor: "pointer" } })}
          columns={[
            { title: "Code", dataIndex: "brandCode" },
            { title: "Brand Name", dataIndex: "brandName" },
            { title: "Status", dataIndex: "status" },
            { title: "Countries", dataIndex: "countryCount" },
            { title: "Stores", dataIndex: "storeCount" },
          ]}
        />
      )}

      {level === "countries" && (
        <Table dataSource={countries} rowKey="id" pagination={false}
          columns={[
            { title: "Country", dataIndex: "countryName" },
            { title: "Code", dataIndex: "countryCode" },
            { title: "Currency", dataIndex: "currencyCode" },
            { title: "Timezone", dataIndex: "timezone" },
            { title: "Tax %", dataIndex: "taxRatePercent" },
            { title: "Stores", dataIndex: "storeCount" },
            { title: "Status", dataIndex: "status" },
          ]}
        />
      )}
    </div>
  );
}
