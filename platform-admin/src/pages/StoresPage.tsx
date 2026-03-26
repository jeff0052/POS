import { Alert, Card, Col, Row, Spin, Table, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { fetchPlatformStoreOverview, type PlatformStoreOverview } from "../api/services/platformStoreService";

export function StoresPage() {
  const [stores, setStores] = useState<PlatformStoreOverview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    void (async () => {
      try {
        const data = await fetchPlatformStoreOverview();
        if (!active) return;
        setStores(data);
        setError(null);
      } catch (err) {
        if (!active) return;
        setError(err instanceof Error ? err.message : "Failed to load platform store overview.");
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  const metrics = useMemo(() => {
    const totalStores = stores.length;
    const totalTables = stores.reduce((sum, store) => sum + store.tableCount, 0);
    const pendingSettlementTables = stores.reduce((sum, store) => sum + store.pendingSettlementTables, 0);
    const merchantCount = new Set(stores.map((store) => store.merchantId)).size;
    return [
      { label: "Merchants", value: merchantCount },
      { label: "Stores", value: totalStores },
      { label: "Tables", value: totalTables },
      { label: "Pending Settlement", value: pendingSettlementTables }
    ];
  }, [stores]);

  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Stores
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        平台侧查看门店开通状态、桌台规模、终端接入和配置覆盖率。
      </Typography.Paragraph>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {metrics.map((item) => (
          <Col span={6} key={item.label}>
            <Card>
              <Typography.Text type="secondary">{item.label}</Typography.Text>
              <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                {item.value}
              </Typography.Title>
            </Card>
          </Col>
        ))}
      </Row>

      <Card>
        {error ? <Alert type="error" showIcon message="Failed to load stores" description={error} style={{ marginBottom: 16 }} /> : null}
        {loading ? (
          <div style={{ display: "flex", justifyContent: "center", padding: "24px 0" }}>
            <Spin />
          </div>
        ) : (
          <Table
            rowKey="storeId"
            dataSource={stores}
            pagination={false}
            columns={[
              { title: "Store", dataIndex: "storeName", key: "storeName" },
              { title: "Store Code", dataIndex: "storeCode", key: "storeCode" },
              { title: "Merchant ID", dataIndex: "merchantId", key: "merchantId" },
              { title: "Tables", dataIndex: "tableCount", key: "tableCount" },
              { title: "Available", dataIndex: "availableTables", key: "availableTables" },
              { title: "Occupied", dataIndex: "occupiedTables", key: "occupiedTables" },
              { title: "Reserved", dataIndex: "reservedTables", key: "reservedTables" },
              { title: "Pending Settlement", dataIndex: "pendingSettlementTables", key: "pendingSettlementTables" }
            ]}
          />
        )}
      </Card>
    </div>
  );
}
