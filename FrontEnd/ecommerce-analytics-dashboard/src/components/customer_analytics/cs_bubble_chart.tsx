import React, { useState, useEffect } from "react";
import {
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  ZAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import apiClient from "../../services/api";

interface CustomerAnalyticsDto {
  topCustomers: TopCustomerData[];
}

interface TopCustomerData {
  customerId: number;
  customerName: string;
  totalSpent: number;
  orderCount: number;
  segment?: string;
}

interface BubbleChartData {
  x: number;
  y: number;
  z: number;
  name: string;
  customerId: number;
  totalSpent: number;
  orderCount: number;
  segment?: string;
}

interface TopCustomersBubbleChartProps {
  startDate: string; // expected format: "YYYY-MM-DD"
  endDate: string;   // expected format: "YYYY-MM-DD"
}

const styles = {
  container: {
    padding: "24px",
    backgroundColor: "white",
    borderRadius: "8px",
    boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
    fontFamily: "Roboto, sans-serif",
  },
  header: {
    marginBottom: "16px",
  },
  title: {
    fontSize: "24px",
    fontWeight: "bold",
    color: "#1f2937",
    marginBottom: "8px",
  },
  subtitle: {
    color: "#6b7280",
    marginBottom: "12px",
    lineHeight: "1.5",
  },
  controlsRow: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    marginBottom: "12px",
  },
  button: {
    padding: "8px 16px",
    backgroundColor: "#3b82f6",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    transition: "background-color 0.2s",
  },
  buttonHover: {
    backgroundColor: "#2563eb",
  },
  legendRow: {
    display: "flex",
    gap: "12px",
    alignItems: "center",
    flexWrap: "wrap" as const,
    marginBottom: "8px",
  },
  legendItem: {
    display: "flex",
    gap: "8px",
    alignItems: "center",
    padding: "6px 8px",
    borderRadius: "6px",
    border: "1px solid #eef2ff",
    background: "#fff",
    boxShadow: "0 1px 2px rgba(0,0,0,0.02)",
    fontSize: "13px",
    color: "#374151",
  },
  swatch: {
    width: "14px",
    height: "14px",
    borderRadius: "3px",
    border: "1px solid rgba(0,0,0,0.06)",
  },
  tooltip: {
    backgroundColor: "white",
    padding: "16px",
    border: "1px solid #d1d5db",
    borderRadius: "8px",
    boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
  },
  tooltipTitle: {
    fontWeight: "600",
    color: "#1f2937",
    marginBottom: "4px",
  },
  tooltipText: {
    fontSize: "14px",
    color: "#6b7280",
    marginBottom: "2px",
  },
};

const TopCustomersBubbleChart: React.FC<TopCustomersBubbleChartProps> = ({
  startDate,
  endDate,
}) => {
  const mockData: CustomerAnalyticsDto = {
    topCustomers: [
      {
        customerId: 2,
        customerName: "Marceline Collins",
        totalSpent: 133474.27,
        orderCount: 332,
        segment: "LOYAL",
      },
      {
        customerId: 1,
        customerName: "Leeann VonRueden",
        totalSpent: 114919.69,
        orderCount: 314,
        segment: "LOYAL",
      },
      {
        customerId: 5,
        customerName: "Raguel Bergstrom",
        totalSpent: 87333.0,
        orderCount: 221,
        segment: "CHAMPION",
      },
    ],
  };

  const [data, setData] = useState<CustomerAnalyticsDto>(mockData);
  const [chartData, setChartData] = useState<BubbleChartData[]>([]);
  const [hoveredButton, setHoveredButton] = useState(false);

  // segment -> color map
  const segmentColors: Record<string, string> = {
    NEW: "#60a5fa",
    LOYAL: "#34d399",
    CHAMPION: "#f59e0b",
    AT_RISK: "#f97316",
  };
  const defaultColor = "#8884d8";

  // build and sanitize chart data, filter invalid points
  useEffect(() => {
    const raw = Array.isArray(data.topCustomers) ? data.topCustomers : [];
    const bubbleData: BubbleChartData[] = raw
      .map((customer) => ({
        x: Number(customer.totalSpent),
        y: Number(customer.orderCount),
        z: Number(customer.totalSpent),
        name: customer.customerName,
        customerId: customer.customerId,
        totalSpent: customer.totalSpent,
        orderCount: customer.orderCount,
        segment: customer.segment,
      }))
      .filter(
        (d) =>
          Number.isFinite(d.x) &&
          Number.isFinite(d.y) &&
          Number.isFinite(d.z) &&
          d.z > 0
      );

    setChartData(bubbleData);
  }, [data]);

  // fetch data when startDate or endDate changes
  useEffect(() => {
    if (!startDate || !endDate) return;

    const fetchData = async () => {
      try {
        const analyticsData = await apiClient.get<CustomerAnalyticsDto>(
          "analytics/customerAnalytics",
          {
            params: {
              start: `${startDate}T00:00:00`,
              end: `${endDate}T23:59:59`,
              threshold:`${startDate}T00:00:00`
            },
          }
        );

        if (!analyticsData) {
          console.warn("analyticsData is undefined (apiClient returned nothing).");
          return;
        }
        if (!Array.isArray((analyticsData as any).topCustomers)) {
          console.warn(
            "analyticsData.topCustomers is missing or not an array:",
            analyticsData
          );
          return;
        }
        setData(analyticsData);
      } catch (error) {
        console.error("Failed to fetch analytics data:", error);
      }
    };

    fetchData();
  }, [startDate, endDate]);

  // tooltip component
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const d: BubbleChartData = payload[0].payload;
      return (
        <div style={styles.tooltip}>
          <p style={styles.tooltipTitle}>{d.name}</p>
          <p style={styles.tooltipText}>Customer ID: {d.customerId}</p>
          <p style={{ ...styles.tooltipText, color: "#3b82f6" }}>
            Total Spent: ${d.totalSpent.toLocaleString()}
          </p>
          <p style={{ ...styles.tooltipText, color: "#10b981" }}>
            Order Count: {d.orderCount}
          </p>
          <p style={{ ...styles.tooltipText, color: "#8b5cf6" }}>
            Avg per Order: ${(d.totalSpent / d.orderCount).toFixed(2)}
          </p>
          <p style={{ ...styles.tooltipText, fontWeight: 600 }}>
            Segment: {d.segment ?? "N/A"}
          </p>
        </div>
      );
    }
    return null;
  };

  const segmentsInData = Array.from(
    new Set(chartData.map((d) => d.segment).filter((s): s is string => Boolean(s)))
  );
  const hasUnknown = chartData.some((d) => !d.segment);
  const segmentsToShow =
    segmentsInData.length > 0 ? segmentsInData : Object.keys(segmentColors);

  const LegendItem: React.FC<{ label: string; color: string }> = ({
    label,
    color,
  }) => (
    <div style={styles.legendItem} role="listitem" aria-label={`${label} segment`}>
      <div style={{ ...styles.swatch, background: color }} aria-hidden />
      <div>{label}</div>
    </div>
  );

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Top Customers Analysis</h2>
        <p style={styles.subtitle}>
          Bubble chart showing customer spending (X-axis) vs order count (Y-axis).
          Bubble size represents total spend.
        </p>

        <div style={styles.controlsRow}>
          <button
            style={{ ...styles.button, ...(hoveredButton ? styles.buttonHover : {}) }}
            onMouseEnter={() => setHoveredButton(true)}
            onMouseLeave={() => setHoveredButton(false)}
            onClick={() => {
              if (startDate && endDate) {
                (async () => {
                  try {
                    const analyticsData = await apiClient.get<CustomerAnalyticsDto>(
                      "analytics/customerAnalytics",
                      {
                        params: {
                          start: `${startDate}T00:00:00`,
                          end: `${endDate}T23:59:59`,
                        },
                      }
                    );
                    if (
                      analyticsData &&
                      Array.isArray((analyticsData as any).topCustomers)
                    ) {
                      setData(analyticsData);
                    }
                  } catch (error) {
                    console.error("Failed to fetch analytics data:", error);
                  }
                })();
              }
            }}
          >
            Refresh Data
          </button>

          <div aria-label="Segment legend" role="list" style={styles.legendRow}>
            {segmentsToShow.map((seg) => (
              <LegendItem key={seg} label={seg} color={segmentColors[seg] ?? defaultColor} />
            ))}

            {hasUnknown && <LegendItem key="unknown" label="Unknown" color={defaultColor} />}
          </div>
        </div>
      </div>

      <ResponsiveContainer width="100%" height={500}>
        <ScatterChart
          margin={{
            top: 20,
            right: 20,
            bottom: 60,
            left: 60,
          }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
          <XAxis
            type="number"
            dataKey="x"
            name="Total Spent"
            domain={["dataMin - 5000", "dataMax + 5000"]}
            tickFormatter={(v) => `$${(v / 1000).toFixed(0)}K`}
            label={{ value: "Total Spent ($)", position: "insideBottom", offset: -10 }}
          />
          <YAxis
            type="number"
            dataKey="y"
            name="Order Count"
            domain={["dataMin - 10", "dataMax + 10"]}
            label={{ value: "Order Count", angle: -90, position: "insideLeft" }}
          />
          <ZAxis type="number" dataKey="z" range={[50, 500]} name="Total Spent" />
          <Tooltip content={<CustomTooltip />} />
          <Scatter data={chartData} fill={defaultColor}>
            {chartData.map((entry) => {
              const color = entry.segment ? segmentColors[entry.segment] ?? defaultColor : defaultColor;
              return <Cell key={`cell-${entry.customerId}`} fill={color} />;
            })}
          </Scatter>
        </ScatterChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TopCustomersBubbleChart;