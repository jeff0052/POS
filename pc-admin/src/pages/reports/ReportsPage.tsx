import { Alert, Card, Skeleton, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { getDashboardSummary, getMemberConsumptionSummary, getSalesReportSummary } from "../../api/services/dashboardService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { MemberConsumptionTopMember } from "../../types";

const topMemberColumns: ColumnsType<MemberConsumptionTopMember> = [
  { title: "Member", dataIndex: "memberName", key: "memberName" },
  { title: "Tier", dataIndex: "tierCode", key: "tierCode", width: 120 },
  { title: "Orders", dataIndex: "orderCount", key: "orderCount", width: 100 },
  { title: "Sales", dataIndex: "totalSales", key: "totalSales", width: 160 },
  { title: "Recharge", dataIndex: "totalRecharge", key: "totalRecharge", width: 160 },
  { title: "Member Discount", dataIndex: "memberDiscount", key: "memberDiscount", width: 180 }
];

export function ReportsPage() {
  const summaryQuery = useAsyncData(getDashboardSummary);
  const salesQuery = useAsyncData(getSalesReportSummary);
  const memberQuery = useAsyncData(getMemberConsumptionSummary);

  const metrics = summaryQuery.data
    ? [
        { label: "Revenue Today", value: summaryQuery.data.revenue },
        { label: "Orders Today", value: summaryQuery.data.orders },
        { label: "Refund Amount", value: summaryQuery.data.refunds },
        { label: "Active Cashiers", value: summaryQuery.data.cashiers }
      ]
    : [];

  const loading = summaryQuery.loading || salesQuery.loading || memberQuery.loading;

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>Reports</Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        Daily operating snapshot for sales, member contribution, recharge behavior, and settlement signals.
      </Typography.Paragraph>

      {summaryQuery.error ? <Alert type="error" message={summaryQuery.error} style={{ marginBottom: 16 }} /> : null}
      {salesQuery.error ? <Alert type="error" message={salesQuery.error} style={{ marginBottom: 16 }} /> : null}
      {memberQuery.error ? <Alert type="error" message={memberQuery.error} style={{ marginBottom: 16 }} /> : null}

      {loading ? (
        <Skeleton active paragraph={{ rows: 10 }} />
      ) : (
        <>
          <Card className="report-hero-card">
            <p className="report-eyebrow">Merchant Insights</p>
            <h3 className="report-hero-title">Today at a glance</h3>
            <p className="report-hero-copy">
              Read the store in one pass, then drill into members and recharge performance below.
            </p>
            <div className="report-kpi-grid">
              {metrics.map((metric) => (
                <div key={metric.label} className="report-kpi-card">
                  <span>{metric.label}</span>
                  <strong>{metric.value}</strong>
                </div>
              ))}
            </div>
          </Card>

          <div className="report-grid report-grid-two">
            <Card className="report-section-card" title="Sales Overview">
              <div className="report-stat-grid">
                <div className="report-stat-card"><span>Total Sales</span><strong>{salesQuery.data?.sales ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Member Sales</span><strong>{salesQuery.data?.memberSales ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Recharge Sales</span><strong>{salesQuery.data?.rechargeSales ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Total Discounts</span><strong>{salesQuery.data?.discounts ?? "-"}</strong></div>
              </div>
            </Card>

            <Card className="report-section-card" title="Member Activity">
              <div className="report-stat-grid">
                <div className="report-stat-card"><span>Member Sales</span><strong>{memberQuery.data?.totalMemberSales ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Member Discounts</span><strong>{memberQuery.data?.totalMemberDiscounts ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Member Orders</span><strong>{memberQuery.data?.memberOrderCount ?? "-"}</strong></div>
                <div className="report-stat-card"><span>Active Members</span><strong>{memberQuery.data?.activeMemberCount ?? "-"}</strong></div>
              </div>
            </Card>
          </div>

          <div className="report-grid report-grid-two report-grid-tight">
            <Card className="report-section-card" title="Recharge Overview">
              <div className="report-list">
                <div className="report-list-row"><span>Recharge Value</span><strong>{memberQuery.data?.totalRecharge ?? "-"}</strong></div>
                <div className="report-list-row"><span>Bonus Issued</span><strong>{memberQuery.data?.totalBonus ?? "-"}</strong></div>
                <div className="report-list-row"><span>Recharge Orders</span><strong>{memberQuery.data?.rechargeOrderCount ?? "-"}</strong></div>
                <div className="report-list-row"><span>Average Recharge</span><strong>{memberQuery.data?.averageRecharge ?? "-"}</strong></div>
              </div>
            </Card>

            <Card className="report-section-card" title="Operational Signals">
              <div className="report-list">
                <div className="report-list-row"><span>Table Turnover</span><strong>{salesQuery.data?.tableTurnover ?? "-"}</strong></div>
                <div className="report-list-row"><span>Pending GTO Batches</span><strong>{salesQuery.data?.pendingGtoBatches ?? "-"}</strong></div>
                <div className="report-list-row"><span>Report Focus</span><strong>Sales · Members · Recharge</strong></div>
                <div className="report-list-row"><span>Status</span><strong>Daily Snapshot Ready</strong></div>
              </div>
            </Card>
          </div>

          <Card className="report-section-card report-table-card" title="Top Members by Sales">
            <Table
              rowKey="memberId"
              columns={topMemberColumns}
              dataSource={memberQuery.data?.topMembers ?? []}
              pagination={false}
              locale={{ emptyText: "No member activity yet" }}
            />
          </Card>
        </>
      )}
    </div>
  );
}
