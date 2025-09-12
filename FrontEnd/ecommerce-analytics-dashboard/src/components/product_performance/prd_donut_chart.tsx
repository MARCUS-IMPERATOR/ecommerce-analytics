import React, { useEffect, useState, useCallback } from "react";
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from "recharts";
import apiClient from "../../services/api";

interface CategoryData {
  category: string;
  totalQuantitySold: number;
  totalRevenue: number;
}

interface ApiResponse {
  categoryPerformance: CategoryData[];
}

interface DonutChartProps {
  startDate: string; // "YYYY-MM-DD"
  endDate: string; // "YYYY-MM-DD"
  lowStockThreshold: number;
}

const CATEGORY_COLORS: Record<string, string> = {
  LAPTOPS: "#506e9a",
  SMARTPHONES: "#2d8bba",
  TABLETS: "#41b8d5",
  OTHERS: "#6ce5e8",
  ACCESSORIES: "#f79c42",
};

const FALLBACK_COLOR = "#cbd5e1";

const CategoryDistributionDonutChart: React.FC<DonutChartProps> = ({
  startDate,
  endDate,
  lowStockThreshold,
}) => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);

    try {
      const payload = await apiClient.get<ApiResponse>(
        `/analytics/productsPerformance?start=${startDate}T00:00:00&end=${endDate}T23:59:59&threshold=${lowStockThreshold}`,
        { signal }
      );

      const categoryData = payload?.categoryPerformance ?? [];

      // Calculate total quantity sold
      const totalSold = categoryData.reduce((sum, category) => sum + category.totalQuantitySold, 0);

      // Map data for Pie chart
      const chartData = categoryData.map((category) => ({
        name: category.category,
        value: category.totalQuantitySold,
        percentage: Math.round((category.totalQuantitySold / totalSold) * 100),
      }));

      setData(chartData);
    } catch (err: unknown) {
      if (err instanceof Error && err.message.toLowerCase().includes("cancel")) {
        return;
      }
      console.error("Error fetching category distribution", err);
      setError(err instanceof Error ? err.message : "Failed to load category data.");
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, lowStockThreshold]);

  useEffect(() => {
    if (!startDate || !endDate) return;

    const controller = new AbortController();
    fetchData(controller.signal);

    return () => controller.abort();
  }, [fetchData, startDate, endDate, lowStockThreshold]);

  const CustomTooltip: React.FC<any> = ({ active, payload }) => {
    if (!active || !payload || !payload.length) return null;
    const d = payload[0].payload;
    return (
      <div style={{ background: "white", border: "1px solid #e2e8f0", borderRadius: 8, padding: 12, boxShadow: "0 8px 25px -2px rgba(0,0,0,0.15)" }}>
        <div style={{ fontWeight: 600, color: "#0f172a", marginBottom: 6 }}>{d.name}</div>
        <div style={{ color: "#64748b" }}>{d.value} units ({d.percentage}%)</div>
      </div>
    );
  };

  const LegendFromData: React.FC = () => {
    if (!data || data.length === 0) return null;
    return (
      <div style={{ display: "flex", flexWrap: "wrap", gap: 12, marginTop: 12, justifyContent: "center" }}>
        {data.map((d) => (
          <div key={d.name} style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <div style={{ width: 14, height: 14, borderRadius: 3, background: CATEGORY_COLORS[d.name] || FALLBACK_COLOR, boxShadow: "0 2px 4px rgba(0,0,0,0.06)" }} />
            <div style={{ fontSize: 13, color: "#334155", fontWeight: 500 }}>
              {d.name} — {d.value} ({d.percentage}%)
            </div>
          </div>
        ))}
      </div>
    );
  };

  const totalSold = data.reduce((s, it) => s + it.value, 0);

  return (
    <div style={{ padding: 12, height: "100%" }}>
      <style>{`
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
      `}</style>

      {loading ? (
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", height: "100%" }}>
          <div style={{ width: 36, height: 36, border: "4px solid #e2e8f0", borderTop: "4px solid #3b82f6", borderRadius: "50%", animation: "spin 1s linear infinite", marginBottom: 12 }} />
          <div style={{ color: "#64748b" }}>Loading category data...</div>
        </div>
      ) : error ? (
        <div style={{ textAlign: "center", padding: 16, background: "#fef2f2", borderRadius: 8, border: "1px solid #fecaca", color: "#b91c1c" }}>
          <div style={{ fontWeight: 700, marginBottom: 8 }}>Unable to Load Data</div>
          <div style={{ marginBottom: 12 }}>{error}</div>
          <button
            onClick={() => fetchData()}
            style={{
              background: "linear-gradient(135deg,#3b82f6,#1d4ed8)",
              color: "white",
              padding: "8px 14px",
              border: "none",
              borderRadius: 8,
              cursor: "pointer",
            }}
          >
            Try Again
          </button>
        </div>
      ) : data.length === 0 ? (
        <div style={{ textAlign: "center", color: "#64748b" }}>
          <div style={{ fontWeight: 700, marginBottom: 8 }}>No Data Available</div>
          <div>No category data found for the selected date range.</div>
        </div>
      ) : (
        <div style={{ width: "100%", height: "100%" }}>
          <div style={{ position: "relative", width: "100%", height: 320 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={72}
                  outerRadius={110}
                  paddingAngle={4}
                  stroke="#fff"
                >
                  {data.map((entry, i) => (
                    <Cell key={`cell-${i}`} fill={CATEGORY_COLORS[entry.name] || FALLBACK_COLOR} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>

            {/* center label */}
            <div style={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%,-50%)",
              textAlign: "center",
              pointerEvents: "none"
            }}>
              <div style={{ fontSize: 18, fontWeight: 700, color: "#0f172a" }}>{totalSold}</div>
              <div style={{ fontSize: 12, color: "#64748b" }}>Total units sold</div>
            </div>
          </div>

          <LegendFromData />
        </div>
      )}
    </div>
  );
};

export default CategoryDistributionDonutChart;