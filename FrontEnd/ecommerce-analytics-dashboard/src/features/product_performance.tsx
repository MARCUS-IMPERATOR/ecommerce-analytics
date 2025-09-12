import React, { useState, useEffect } from "react";
import TurnoverRateChart from "../components/product_performance/prd_bar_chart";
import CategoryDistributionDonutChart from "../components/product_performance/prd_donut_chart";
import CategoryPerformanceChart from "../components/product_performance/prd_stacked_bar_chart";
import LowStockTable from "../components/product_performance/prd_table";

const presets = [
  { key: "last_7", label: "Last 7 days" },
  { key: "last_30", label: "Last 30 days" },
  { key: "last_90", label: "Last 90 days" },
  { key: "this_month", label: "This month" },
  { key: "custom", label: "Custom range" },
];

const ProductPerformance: React.FC = () => {
  const [selectedPreset, setSelectedPreset] = useState<string>("last_30");
  const [startDateInput, setStartDateInput] = useState<string>("");
  const [endDateInput, setEndDateInput] = useState<string>("");

  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");

  // Low stock threshold
  const [lowStockThreshold, setLowStockThreshold] = useState<number>(10);

  // Spinner
  const spinnerStyle: React.CSSProperties = {
    border: "4px solid #f3f3f3",
    borderTop: "4px solid #3b82f6",
    borderRadius: "50%",
    width: 40,
    height: 40,
    animation: "spin 1s linear infinite",
    margin: "auto",
  };
  const spinnerContainerStyle: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    height: "100%",
  };

  // Inject CSS
  useEffect(() => {
    const styleEl = document.createElement("style");
    styleEl.innerHTML = `
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
      @media (max-width: 1024px) {
        .chartsGrid {
          grid-template-columns: 1fr !important;
        }
      }
    `;
    document.head.appendChild(styleEl);
    return () => document.head.removeChild(styleEl);
  }, []);

  const toYMD = (d: Date) => d.toISOString().slice(0, 10);

  const computePresetDates = (presetKey: string) => {
    const now = new Date();
    let start: Date;
    let end = new Date(now);

    switch (presetKey) {
      case "last_7":
        start = new Date(now);
        start.setDate(now.getDate() - 7);
        break;
      case "last_30":
        start = new Date(now);
        start.setDate(now.getDate() - 30);
        break;
      case "last_90":
        start = new Date(now);
        start.setDate(now.getDate() - 90);
        break;
      case "this_month":
        start = new Date(now.getFullYear(), now.getMonth(), 1);
        end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        break;
      default:
        return;
    }
    setStartDate(toYMD(start));
    setEndDate(toYMD(end));
  };

  useEffect(() => {
    if (selectedPreset !== "custom") computePresetDates(selectedPreset);
  }, [selectedPreset]);

  const applyCustomRange = () => {
    if (!startDateInput || !endDateInput) {
      alert("Please select both start and end dates.");
      return;
    }
    if (startDateInput > endDateInput) {
      alert("Start date must be before or equal to end date.");
      return;
    }
    setStartDate(startDateInput);
    setEndDate(endDateInput);
  };

  const containerStyle: React.CSSProperties = {
    fontFamily: "Roboto",
    padding: 32,
    backgroundColor: "#f8fafc",
  };

  const headerStyle: React.CSSProperties = {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 32,
    flexWrap: "wrap",
    gap: 16,
  };

  const titleStyle: React.CSSProperties = {
    fontSize: 32,
    fontWeight: 700,
    color: "#1e293b",
    margin: 0,
    letterSpacing: "-0.025em",
  };

  const dateControlsStyle: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    gap: 12,
    backgroundColor: "white",
    padding: 16,
    borderRadius: 12,
    boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
    border: "1px solid #e2e8f0",
    flexWrap: "wrap",
  };

  const labelStyle: React.CSSProperties = {
    fontWeight: 600,
    color: "#374151",
    fontSize: 14,
    whiteSpace: "nowrap",
  };

  const selectStyle: React.CSSProperties = {
    padding: "10px 12px",
    borderRadius: 8,
    border: "2px solid #e2e8f0",
    backgroundColor: "white",
    fontSize: 14,
    fontWeight: 500,
    color: "#374151",
    outline: "none",
    cursor: "pointer",
    minWidth: 140,
  };

  const inputStyle: React.CSSProperties = {
    padding: "10px 12px",
    borderRadius: 8,
    border: "2px solid #e2e8f0",
    backgroundColor: "white",
    fontSize: 14,
    color: "#374151",
    outline: "none",
    fontFamily: "inherit",
    width: 100,
  };

  const buttonStyle: React.CSSProperties = {
    padding: "10px 20px",
    borderRadius: 8,
    backgroundColor: "#3b82f6",
    color: "white",
    border: "none",
    cursor: "pointer",
    fontSize: 14,
    fontWeight: 600,
  };

  const separatorStyle: React.CSSProperties = {
    color: "#6b7280",
    fontSize: 14,
    fontWeight: 500,
  };

  const customRangeStyle: React.CSSProperties = { display: "flex", gap: 8, alignItems: "center" };

  const chartsContainerStyle: React.CSSProperties = {
    display: "grid",
    gridTemplateColumns: "repeat(2, minmax(200px, 1fr))",
    gap: 20,
  };

  const chartCardStyle: React.CSSProperties = {
    backgroundColor: "white",
    borderRadius: 12,
    padding: 0,
    boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    minWidth: "200px",
    flex: "1 1 auto",
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h1 style={titleStyle}>Product Performance</h1>

        <div style={dateControlsStyle}>
          {/* Date Preset Selector */}
          <label htmlFor="presetSelect" style={labelStyle}>
            Date range:
          </label>
          <select
            id="presetSelect"
            value={selectedPreset}
            onChange={(e) => setSelectedPreset(e.target.value)}
            style={selectStyle}
          >
            {presets.map((p) => (
              <option key={p.key} value={p.key}>
                {p.label}
              </option>
            ))}
          </select>

          {/* Custom Date Range */}
          {selectedPreset === "custom" && (
            <div style={customRangeStyle}>
              <input
                type="date"
                value={startDateInput}
                onChange={(e) => setStartDateInput(e.target.value)}
                style={inputStyle}
              />
              <span style={separatorStyle}>—</span>
              <input
                type="date"
                value={endDateInput}
                onChange={(e) => setEndDateInput(e.target.value)}
                style={inputStyle}
              />
              <button style={buttonStyle} onClick={applyCustomRange}>
                Apply
              </button>
            </div>
          )}

          {/* Low Stock Threshold Input */}
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <label htmlFor="lowStockThreshold" style={labelStyle}>
              Low stock threshold:
            </label>
            <input
              id="lowStockThreshold"
              type="number"
              min={1}
              value={lowStockThreshold}
              onChange={(e) => setLowStockThreshold(Number(e.target.value))}
              style={inputStyle}
            />
          </div>
        </div>
      </div>

      <div className="chartsGrid" style={chartsContainerStyle}>
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <TurnoverRateChart startDate={startDate} endDate={endDate} lowStockThreshold={lowStockThreshold} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>

        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <LowStockTable startDate={startDate} endDate={endDate} lowStockthreshold={lowStockThreshold} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>
        
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <CategoryPerformanceChart startDate={startDate} endDate={endDate} lowStockThreshold={lowStockThreshold} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>

        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <CategoryDistributionDonutChart startDate={startDate} endDate={endDate} lowStockThreshold={lowStockThreshold} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProductPerformance;