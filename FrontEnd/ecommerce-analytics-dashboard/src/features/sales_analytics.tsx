import React, { useState, useEffect } from "react";
import MonthlySalesChart from "../components/sales_analytics/sal_bar_chart";
import SalesHeatmap from "../components/sales_analytics/sal_heat_map";

const presets = [
  { key: "this_year", label: "This year" },
  { key: "last_year", label: "Last year" },
  { key: "custom", label: "Custom range" },
];

const SalesAnalytics: React.FC = () => {
  const [selectedPreset, setSelectedPreset] = useState<string>("this_year");
  const [startDateInput, setStartDateInput] = useState<string>("");
  const [endDateInput, setEndDateInput] = useState<string>("");

  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");

  // Inject CSS with better responsive design
  useEffect(() => {
    const styleEl = document.createElement("style");
    styleEl.innerHTML = `
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
      
      .chartsGrid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
        gap: 20px;
        width: 100%;
      }
      
      @media (max-width: 1200px) {
        .chartsGrid {
          grid-template-columns: 1fr;
        }
      }
      
      @media (max-width: 576px) {
        .chartsGrid {
          grid-template-columns: 1fr;
          gap: 16px;
        }
        
        .date-controls {
          flex-direction: column;
          align-items: stretch !important;
        }
        
        .custom-range {
          flex-direction: column;
          gap: 12px !important;
        }
        
        .header {
          flex-direction: column;
          align-items: stretch !important;
          gap: 20px !important;
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
      case "this_year":
        start = new Date(now.getFullYear(), 0, 1);
        break;
      case "last_year":
        start = new Date(now.getFullYear() - 1, 0, 1);
        end = new Date(now.getFullYear(), 0, 1);
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
    padding: "32px",
    backgroundColor: "#f8fafc",
    minHeight: "100vh",
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
    fontSize: "clamp(24px, 5vw, 32px)", // Responsive font size
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
    minWidth: 140,
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
    whiteSpace: "nowrap",
  };

  const separatorStyle: React.CSSProperties = {
    color: "#6b7280",
    fontSize: 14,
    fontWeight: 500,
  };

  const customRangeStyle: React.CSSProperties = { 
    display: "flex", 
    gap: 8, 
    alignItems: "center",
    flexWrap: "wrap",
  };

  const chartCardStyle: React.CSSProperties = {
    backgroundColor: "white",
    borderRadius: 12,
    padding: 16,
    boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
    border: "1px solid #e2e8f0",
    width: "100%",
    minHeight: 400,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  };

  const spinnerStyle: React.CSSProperties = {
    border: "4px solid #f3f3f3",
    borderTop: "4px solid #3b82f6",
    borderRadius: "50%",
    width: 40,
    height: 40,
    animation: "spin 1s linear infinite",
  };

  const spinnerContainerStyle: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    height: "300px",
    width: "100%",
  };

  return (
    <div style={containerStyle}>
      <div className="header" style={headerStyle}>
        <h1 style={titleStyle}>Sales Analytics</h1>

        <div className="date-controls" style={dateControlsStyle}>
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
            <div className="custom-range" style={customRangeStyle}>
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

      <div className="chartsGrid">
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <MonthlySalesChart startDate={startDate} endDate={endDate}/>
          ) : (
            <div style={spinnerContainerStyle}>
              <div style={spinnerStyle}></div>
            </div>
          )}
        </div>
        <div style={chartCardStyle}>
          {startDate && endDate ? (
            <SalesHeatmap startDate={startDate} endDate={endDate}/>
          ) : (
            <div style={spinnerContainerStyle}>
              <div style={spinnerStyle}></div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SalesAnalytics;