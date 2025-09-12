import React, { useState, useEffect, useCallback } from "react";
import KPICard from "../components/KpiCard";
import apiClient from "../services/api";
import { Margin } from "@mui/icons-material";

const presets = [
  { key: "last_7", label: "Last 7 days" },
  { key: "last_30", label: "Last 30 days" },
  { key: "last_90", label: "Last 90 days" },
  { key: "this_month", label: "This month" },
  { key: "custom", label: "Custom range" },
];

const Kpi = () => {
  const [kpiData, setKpiData] = useState(null);
  const [customerData, setCustomerData] = useState(null);
  const [productCount, setProductCount] = useState(null);
  const [previousKpiData, setPreviousKpiData] = useState(null);
  const [previousCustomerData, setPreviousCustomerData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedPreset, setSelectedPreset] = useState("last_30");
  const [startDateInput, setStartDateInput] = useState("");
  const [endDateInput, setEndDateInput] = useState("");

  // format date to yyyy-MM-ddTHH:mm:ss
  const formatDate = useCallback((date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }, []);

  // Helper function to calculate previous period dates
  const computePreviousPeriod = useCallback((start, end) => {
    const periodLength = end.getTime() - start.getTime();
    const previousEnd = new Date(start.getTime() - 1); // 1ms before current start
    const previousStart = new Date(previousEnd.getTime() - periodLength);
    return { previousStart, previousEnd };
  }, []);

  // Helper function to calculate trend
  const calculateTrend = useCallback((current, previous) => {
    if (previous === 0 || previous === null || previous === undefined) {
      // If no previous data (or previous is zero) we can't compute a meaningful percent change
      if (current === 0 || current === null || current === undefined) {
        return { trend: "NEUTRAL", trendValue: 0 };
      }
      // if previous is 0 but current > 0, show UP with 100% (could be infinite but pragmatic)
      return { trend: "UP", trendValue: 100 };
    }
    const change = ((current - previous) / previous) * 100;
    const trend = change > 0 ? "UP" : change < 0 ? "DOWN" : "NEUTRAL";
    const trendValue = Math.abs(change);
    return { trend, trendValue: Math.round(trendValue * 10) / 10 };
  }, []);

  const computeRangeFromPreset = useCallback((presetKey) => {
    const now = new Date();
    let start = new Date();
    let end = new Date();
    switch (presetKey) {
      case "last_7":
        start.setDate(now.getDate() - 7);
        break;
      case "last_30":
        start.setDate(now.getDate() - 30);
        break;
      case "last_90":
        start.setDate(now.getDate() - 90);
        break;
      case "this_month":
        start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
        end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
        break;
      case "custom":
        return { start: null, end: null };
      default:
        start.setDate(now.getDate() - 30);
    }
    if (presetKey !== "this_month" && presetKey !== "last_30") {
      start.setHours(0, 0, 0, 0);
      end = new Date();
      // To be safe set end seconds to 59
      end.setSeconds(59, 999);
    }
    return { start, end };
  }, []);

  // centralized fetch function, used by effect and on Apply
  const fetchKpiRange = useCallback(
    async (start, end) => {
      setLoading(true);
      setError(null);
      try {
        const threshold = new Date();
        threshold.setDate(threshold.getDate() - 7);

        // Calculate previous period for trend comparison
        const { previousStart, previousEnd } = computePreviousPeriod(start, end);

        console.log("Fetching KPI data with dates:", {
          current: { start: formatDate(start), end: formatDate(end) },
          previous: { start: formatDate(previousStart), end: formatDate(previousEnd) },
          threshold: formatDate(threshold),
        });

        // Fetch current period data
        const kpiResponse = await apiClient.get(
          `/analytics/kpi?start=${formatDate(start)}&end=${formatDate(end)}`
        );
        const kpiResult = kpiResponse.data || kpiResponse;
        setKpiData(kpiResult);

        // Fetch previous period data for trend comparison
        try {
          const prevKpiResponse = await apiClient.get(
            `/analytics/kpi?start=${formatDate(previousStart)}&end=${formatDate(previousEnd)}`
          );
          const prevKpiResult = prevKpiResponse.data || prevKpiResponse;
          setPreviousKpiData(prevKpiResult);
        } catch (prevErr) {
          console.warn("Previous KPI data unavailable, trends will not be calculated", prevErr);
          setPreviousKpiData(null);
        }

        // Customers
        try {
          const custResponse = await apiClient.get(
            `/analytics/customerAnalytics?start=${formatDate(start)}&end=${formatDate(end)}&threshold=${formatDate(threshold)}`
          );
          setCustomerData(custResponse.data || custResponse);

          // Fetch previous customer data
          try {
            const prevCustResponse = await apiClient.get(
              `/analytics/customerAnalytics?start=${formatDate(previousStart)}&end=${formatDate(previousEnd)}&threshold=${formatDate(threshold)}`
            );
            setPreviousCustomerData(prevCustResponse.data || prevCustResponse);
          } catch (prevCustErr) {
            console.warn(
              "Previous customer data unavailable, customer trends will not be calculated",
              prevCustErr
            );
            setPreviousCustomerData(null);
          }
        } catch (custErr) {
          console.warn("Customer data unavailable, continuing with other data", custErr);
          setCustomerData(null);
          setPreviousCustomerData(null);
        }

        // Products
        try {
          const prodResponse = await apiClient.get(
            `/analytics/productsPerformance?start=${formatDate(start)}&end=${formatDate(end)}&threshold=10`
          );
          const prodResult = prodResponse.data || prodResponse;
          setProductCount(prodResult?.topProducts?.length || 0);
        } catch (prodErr) {
          console.warn("Product data unavailable, continuing with other data", prodErr);
          setProductCount(0);
        }

        setLoading(false);
      } catch (err) {
        console.error("General Error while fetching KPI data:", err);
        setError(err.message || "Failed to fetch KPI data");
        setLoading(false);
      }
    },
    [computePreviousPeriod, formatDate]
  );

  // initial load (use default preset)
  useEffect(() => {
    const { start, end } = computeRangeFromPreset(selectedPreset);
    if (selectedPreset === "custom") {
      // keep inputs blank for custom until user chooses
      setStartDateInput("");
      setEndDateInput("");
      setLoading(false);
      return;
    }
    fetchKpiRange(start, end);
  }, [computeRangeFromPreset, fetchKpiRange, selectedPreset]);

  // Apply button for custom ranges (user picks yyyy-mm-dd inputs)
  const applyCustomRange = () => {
    if (!startDateInput || !endDateInput) {
      setError("Please select both start and end dates for a custom range.");
      return;
    }
    // Create Date objects from the yyyy-mm-dd inputs
    // Start at 00:00:00, end at 23:59:59 to include the whole day
    const startParts = startDateInput.split("-").map(Number);
    const endParts = endDateInput.split("-").map(Number);
    const start = new Date(startParts[0], startParts[1] - 1, startParts[2], 0, 0, 0);
    const end = new Date(endParts[0], endParts[1] - 1, endParts[2], 23, 59, 59);

    // basic validation
    if (start > end) {
      setError("Start date must be before the end date.");
      return;
    }
    setError(null);
    fetchKpiRange(start, end);
  };

  useEffect(() => {
    console.log("Current state:", {
      kpiData,
      customerData,
      productCount,
      previousKpiData,
      previousCustomerData,
      loading,
      error,
    });
  }, [kpiData, customerData, productCount, previousKpiData, previousCustomerData, loading, error]);

  const containerStyle = {
    fontFamily: "Roboto",
    padding: "32px",
    marginBottom: 0,
    backgroundColor: "#f8fafc",
  };

  const headerStyle = {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "32px",
    flexWrap: "wrap",
    gap: "16px",
  };

  const titleStyle = {
    fontSize: "32px",
    fontWeight: "700",
    color: "#1e293b",
    margin: "0",
    letterSpacing: "-0.025em",
  };

  const dateControlsStyle = {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    backgroundColor: "white",
    padding: "16px",
    borderRadius: "12px",
    boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
    border: "1px solid #e2e8f0",
  };

  const labelStyle = { fontWeight: "600", color: "#374151", fontSize: "14px", whiteSpace: "nowrap" };
  const selectStyle = {
    padding: "10px 12px",
    borderRadius: "8px",
    border: "2px solid #e2e8f0",
    backgroundColor: "white",
    fontSize: "14px",
    fontWeight: "500",
    color: "#374151",
    outline: "none",
    transition: "all 0.2s ease",
    cursor: "pointer",
    minWidth: "140px",
  };
  const inputStyle = {
    padding: "10px 12px",
    borderRadius: "8px",
    border: "2px solid #e2e8f0",
    backgroundColor: "white",
    fontSize: "14px",
    color: "#374151",
    outline: "none",
    transition: "all 0.2s ease",
    fontFamily: "inherit",
  };
  const buttonStyle = {
    padding: "10px 20px",
    borderRadius: "8px",
    backgroundColor: "#3b82f6",
    color: "white",
    border: "none",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.2s ease",
    boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  };
  const separatorStyle = { color: "#6b7280", fontSize: "14px", fontWeight: "500" };
  const customRangeStyle = { display: "flex", gap: "8px", alignItems: "center" };
  const kpiGridStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    gap: "24px",
  };

  if (loading) {
    return (
      <div style={containerStyle}>
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            height: "400px",
            fontSize: "18px",
            color: "#6b7280",
            backgroundColor: "white",
            borderRadius: "12px",
            boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1)",
          }}
        >
          <div style={{ textAlign: "center" }}>
            <div style={{ marginBottom: "12px" }}>📊</div>
            Loading KPI data...
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={containerStyle}>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            height: "400px",
            fontSize: "18px",
            color: "#ef4444",
            padding: "32px",
            textAlign: "center",
            backgroundColor: "white",
            borderRadius: "12px",
            boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1)",
          }}
        >
          <div style={{ marginBottom: "12px", fontSize: "24px" }}>⚠️</div>
          <div style={{ marginBottom: "16px", fontWeight: "600" }}>Error: {error}</div>
          <button
            onClick={() => window.location.reload()}
            style={{ ...buttonStyle, backgroundColor: "#ef4444" }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = "#dc2626";
              e.target.style.transform = "translateY(-1px)";
              e.target.style.boxShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1)";
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = "#ef4444";
              e.target.style.transform = "translateY(0)";
              e.target.style.boxShadow = "0 1px 2px 0 rgba(0, 0, 0, 0.05)";
            }}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const totalSalesTrend = calculateTrend(kpiData?.totalRevenue, previousKpiData?.totalRevenue);
  const newCustomersTrend = calculateTrend(kpiData?.newCustomersCount, previousKpiData?.newCustomersCount);
  const avgOrderValueTrend = calculateTrend(kpiData?.averageOrderValue, previousKpiData?.averageOrderValue);
  const clvTrend = calculateTrend(customerData?.averageCustomerLifetimeValue, previousCustomerData?.averageCustomerLifetimeValue);
  const totalOrdersTrend = calculateTrend(kpiData?.totalOrders, previousKpiData?.totalOrders);
  // For churn rate, lower is better, so we invert the trend logic
  const churnTrend = calculateTrend(previousCustomerData?.churnRate, customerData?.churnRate);
  const totalCustomersTrend = calculateTrend(kpiData?.totalCustomers, previousKpiData?.totalCustomers);
  const totalProductsTrend = calculateTrend(kpiData?.totalProducts, previousKpiData?.totalProducts);

  // Compose KPI cards with dynamic trends
  const kpiCards = [
    {
      title: "Total Sales",
      value: kpiData?.totalRevenue || 0,
      trend: totalSalesTrend.trend,
      trendValue: totalSalesTrend.trendValue,
      icon: "money",
      theme: "green",
      format: "currency",
    },
    {
      title: "New Customers",
      value: kpiData?.newCustomersCount || 0,
      trend: newCustomersTrend.trend,
      trendValue: newCustomersTrend.trendValue,
      icon: "customers",
      theme: "blue",
      format: "number",
    },
    {
      title: "Average Order Value",
      value: kpiData?.averageOrderValue || 0,
      trend: avgOrderValueTrend.trend,
      trendValue: avgOrderValueTrend.trendValue,
      icon: "order",
      theme: "purple",
      format: "currency",
    },
    {
      title: "Customer Lifetime Value",
      value: customerData?.averageCustomerLifetimeValue || 0,
      trend: clvTrend.trend,
      trendValue: clvTrend.trendValue,
      icon: "champ",
      theme: "gold",
      format: "currency",
    },
    {
      title: "Total Orders",
      value: kpiData?.totalOrders || 0,
      trend: totalOrdersTrend.trend,
      trendValue: totalOrdersTrend.trendValue,
      icon: "order",
      theme: "blue",
      format: "number",
    },
    {
      title: "Churn Rate",
      value: customerData?.churnRate || 0,
      trend: churnTrend.trend,
      trendValue: churnTrend.trendValue,
      icon: "down",
      theme: "orange",
      format: "percentage",
    },
    {
      title: "Total Customers",
      value: kpiData?.totalCustomers || 0,
      trend: totalCustomersTrend.trend,
      trendValue: totalCustomersTrend.trendValue,
      icon: "customers",
      theme: "green",
      format: "number",
    },
    {
      title: "Total Products",
      value: kpiData?.totalProducts || 0,
      trend: totalProductsTrend.trend,
      trendValue: totalProductsTrend.trendValue,
      icon: "product",
      theme: "purple",
      format: "number",
    },
  ];

  return (
    <div style={containerStyle}>
      {/* Header with title and date controls */}
      <div style={headerStyle}>
        <h1 style={titleStyle}> Global KPIs </h1>
        <div style={dateControlsStyle}>
          <label htmlFor="presetSelect" style={labelStyle}>
            Date range:
          </label>
          <select
            id="presetSelect"
            value={selectedPreset}
            onChange={(e) => setSelectedPreset(e.target.value)}
            style={selectStyle}
            onMouseEnter={(e) => {
              e.target.style.borderColor = "#3b82f6";
            }}
            onMouseLeave={(e) => {
              e.target.style.borderColor = "#e2e8f0";
            }}
            onFocus={(e) => {
              e.target.style.borderColor = "#3b82f6";
              e.target.style.boxShadow = "0 0 0 3px rgba(59, 130, 246, 0.1)";
            }}
            onBlur={(e) => {
              e.target.style.borderColor = "#e2e8f0";
              e.target.style.boxShadow = "none";
            }}
          >
            {presets.map((p) => (
              <option key={p.key} value={p.key}>
                {p.label}
              </option>
            ))}
          </select>

          {selectedPreset === "custom" && (
            <div style={customRangeStyle}>
              <input
                type="date"
                value={startDateInput}
                onChange={(e) => setStartDateInput(e.target.value)}
                style={inputStyle}
                onFocus={(e) => {
                  e.target.style.borderColor = "#3b82f6";
                  e.target.style.boxShadow = "0 0 0 3px rgba(59, 130, 246, 0.1)";
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = "#e2e8f0";
                  e.target.style.boxShadow = "none";
                }}
              />
              <span style={separatorStyle}>—</span>
              <input
                type="date"
                value={endDateInput}
                onChange={(e) => setEndDateInput(e.target.value)}
                style={inputStyle}
                onFocus={(e) => {
                  e.target.style.borderColor = "#3b82f6";
                  e.target.style.boxShadow = "0 0 0 3px rgba(59, 130, 246, 0.1)";
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = "#e2e8f0";
                  e.target.style.boxShadow = "none";
                }}
              />
              <button
                onClick={applyCustomRange}
                style={buttonStyle}
                onMouseEnter={(e) => {
                  e.target.style.backgroundColor = "#2563eb";
                  e.target.style.transform = "translateY(-1px)";
                  e.target.style.boxShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1)";
                }}
                onMouseLeave={(e) => {
                  e.target.style.backgroundColor = "#3b82f6";
                  e.target.style.transform = "translateY(0)";
                  e.target.style.boxShadow = "0 1px 2px 0 rgba(0, 0, 0, 0.05)";
                }}
              >
                Apply
              </button>
            </div>
          )}
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div style={kpiGridStyle}>
        {kpiCards.map((card, idx) => (
          <KPICard key={idx} {...card} />
        ))}
      </div>
    </div>
  );
};

export default Kpi;