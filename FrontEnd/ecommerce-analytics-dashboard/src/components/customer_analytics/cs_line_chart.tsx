import React, { useState, useEffect } from "react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import apiClient from "../../services/api";

interface RegistrationTrendData {
  year: number;
  month: number;
  registrationCount: number;
}

interface CustomerAnalyticsData {
  registrationTrends: RegistrationTrendData[];
}

interface LineChartProps {
  startDate: string;
  endDate: string;
  thresholdDate: string;
}

const RegistrationTrendsChart: React.FC<LineChartProps> = ({ startDate, endDate, thresholdDate }) => {
  const [data, setData] = useState<RegistrationTrendData[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);

    try {
        const response = await apiClient.get<CustomerAnalyticsData>(
        `analytics/customerAnalytics?start=${startDate}T00:00:00&end=${endDate}T23:59:59&threshold=${thresholdDate}T00:00:00`
        );

        if (!response || !response.registrationTrends?.length) {
        setData([]);
        return;
        }

        const transformed = response.registrationTrends
        .map((t) => ({
            ...t,
            date: new Date(t.year, t.month - 1).toLocaleDateString("en-US", { month: "short", year: "numeric" }),
        }))
        .sort((a, b) => (a.year !== b.year ? a.year - b.year : a.month - b.month));

        setData(transformed);
    } catch (err: any) {
        setError(err.message || "Failed to load registration trends");
        setData([]);
    } finally {
        setLoading(false);
    }
    };

  useEffect(() => {
    fetchData();
  }, [startDate, endDate, thresholdDate]);

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div style={{ background: "#fff", padding: 12, borderRadius: 8, border: "1px solid #e2e8f0", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
          <p style={{ margin: 0, fontWeight: 600, color: "#1e293b" }}>{label}</p>
          <p style={{ margin: 0, color: "#2563eb" }}>Registrations: {payload[0].value}</p>
        </div>
      );
    }
    return null;
  };

  if (loading) return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "400px", backgroundColor: "#f8fafc", borderRadius: 12 }}>
      <div style={{ textAlign: "center" }}>
        <div style={{ width: 40, height: 40, border: "4px solid #e2e8f0", borderTop: "4px solid #2563eb", borderRadius: "50%", animation: "spin 1s linear infinite", margin: "0 auto 12px" }} />
        <p style={{ color: "#64748b" }}>Loading registration trends...</p>
      </div>
      <style>{`@keyframes spin {0% { transform: rotate(0deg);} 100% { transform: rotate(360deg);} }`}</style>
    </div>
  );

  if (error) return (
    <div style={{ padding: 24, borderRadius: 12, backgroundColor: "#fef2f2", border: "1px solid #fecaca", textAlign: "center" }}>
      <h3 style={{ color: "#b91c1c" }}>Failed to load data</h3>
      <p style={{ color: "#991b1b" }}>{error}</p>
      <button onClick={fetchData} style={{ marginTop: 12, padding: "8px 16px", borderRadius: 8, border: "none", backgroundColor: "#2563eb", color: "#fff", cursor: "pointer", fontWeight: 600 }}>Retry</button>
    </div>
  );

  if (!data.length) return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "280px", backgroundColor: "#f8fafc", borderRadius: 12 }}>
      <div style={{ textAlign: "center" }}>
        <p style={{ margin: 0, fontWeight: 600, color: "#374151" }}>No registration data available</p>
        <p style={{ marginTop: 8, color: "#6b7280" }}>The server returned no registration trends for this range.</p>
      </div>
    </div>
  );

  return (
    <div style={{ width: "100%", height: 400, backgroundColor: "#fff", borderRadius: 12, padding: 12, boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}>
      <h3 style={{ fontSize: 20, fontWeight: 700, color: "#1e293b", marginBottom: 12 }}>Customer Registration Trends</h3>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="date" stroke="#6b7280" fontSize={12} angle={-45} textAnchor="end" height={60} interval={0} />
          <YAxis stroke="#6b7280" fontSize={12} label={{ value: "Number of Registrations", angle: -90, position: "insideLeft", style: { textAnchor: "middle" } }} />
          <Tooltip content={<CustomTooltip />} />
          <Legend />
          <Line type="monotone" dataKey="registrationCount" stroke="#2563eb" strokeWidth={3} dot={{ fill: "#2563eb", r: 5 }} activeDot={{ r: 7, fill: "#1d4ed8" }} name="Monthly Registrations" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default RegistrationTrendsChart;