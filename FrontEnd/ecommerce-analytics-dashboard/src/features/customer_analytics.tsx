import React, { useState, useEffect } from "react";
import TopCustomersBubbleChart from "../components/customer_analytics/cs_bubble_chart";
import SegmentsDistributionDonutChart from "../components/customer_analytics/cs_donut_chart";
import RegistrationTrendsChart from "../components/customer_analytics/cs_line_chart";
import TopCustomersTable from "../components/customer_analytics/cs_table";

const presets = [
  { key: "last_7", label: "Last 7 days" },
  { key: "last_30", label: "Last 30 days" },
  { key: "last_90", label: "Last 90 days" },
  { key: "this_month", label: "This month" },
  { key: "custom", label: "Custom range" },
];

const CustomerAnalytics: React.FC = () => {
  const [selectedPreset, setSelectedPreset] = useState<string>("last_30");
  const [startDateInput, setStartDateInput] = useState<string>("");
  const [endDateInput, setEndDateInput] = useState<string>("");

  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");

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

  const getThresholdDate = (): string => {
    const now = new Date();
    now.setDate(now.getDate() - 30);
    return toYMD(now);
  };
  const thresholdDate = getThresholdDate();

  const containerStyle: React.CSSProperties = {
    fontFamily: "Roboto",
    padding: 32,
    backgroundColor: "#f8fafc",
    // minHeight: "100vh",
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

  // Grid layout
  const chartsContainerStyle: React.CSSProperties = {
    display: "grid",
    gridTemplateColumns: "repeat(2, minmax(200px, 1fr))",  // Makes the columns flexible based on content size
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
    
    minWidth: "200px",  // Optional: Make sure the cards don’t shrink too much
    flex: "1 1 auto",    // This ensures the card can grow but not shrink
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h1 style={titleStyle}>Customer Analytics</h1>

        <div style={dateControlsStyle}>
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
        </div>
      </div>

      <div className="chartsGrid" style={chartsContainerStyle}>
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <TopCustomersTable startDate={startDate} endDate={endDate} thresholdDate={thresholdDate} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <TopCustomersBubbleChart startDate={startDate} endDate={endDate} thresholdDate={thresholdDate} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>

        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <RegistrationTrendsChart startDate={startDate} endDate={endDate} thresholdDate={thresholdDate} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>

        
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <SegmentsDistributionDonutChart startDate={startDate} endDate={endDate} thresholdDate={thresholdDate} />
          ) : (
            <div style={spinnerContainerStyle}><div style={spinnerStyle}></div></div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CustomerAnalytics;