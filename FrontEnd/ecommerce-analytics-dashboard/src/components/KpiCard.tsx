import React, { useState } from "react";
import { ICON_MAP } from "./icon_map";
import { THEMES } from "./themes";
import { COLORS } from "../styles/colors";

const KPICard = ({title, value, trend, trendValue, icon, theme = "blue", format }) => {
  const [isHovered, setIsHovered] = useState(false);

  const IconCandidate = ICON_MAP[icon];
  const Theme = THEMES[theme] || THEMES.blue;
  const isPositive = trend === "UP";
  const TrendCandidate = isPositive ? ICON_MAP.up : ICON_MAP.down;

  const RenderIcon = ({ candidate, size = 24, alt = "" }) => {
    if (!candidate) return null;
    if (typeof candidate === "string") {
      return <img src={candidate} alt={alt} style={{ width: size, height: size }} />;
    }
    // React component (SVG imported as ReactComponent)
    const Cmp = candidate;
    return <Cmp width={size} height={size} />;
  };

  const formatValue = (val, fmt) => {
    if (fmt === "currency") {
      // show 0 decimals for integers, 2 decimals otherwise, with locale formatting
      const nf = new Intl.NumberFormat(undefined, { style: "currency", currency: "USD", maximumFractionDigits: 2 });
      return nf.format(Number(val));
    }
    if (fmt === "percentage") {
      return `${Number(val).toLocaleString(undefined, { maximumFractionDigits: 2 })}%`;
    }
    return val != null ? String(val) : "-";
  };

  const styles = {
    card: {
      backgroundColor: "white",
      borderRadius: "8px",
      padding: "24px",
      boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
      border: "1px solid #e5e7eb",
      transition: "box-shadow 0.2s ease",
      cursor: "default"
    },
    cardHover: {
      boxShadow: "0 6px 18px rgba(0,0,0,0.12)"
    },
    header: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      marginBottom: "16px"
    },
    iconContainer: {
      padding: "12px",
      borderRadius: "8px",
      display: "flex",
      alignItems: "center",
      justifyContent: "center"
    },
    valueContainer: {
      textAlign: "right"
    },
    value: {
      fontSize: "28px",
      fontWeight: "700",
      color: "#111827",
      margin: 0
    },
    title: {
      color: "#6b7280",
      fontSize: "14px",
      fontWeight: "500",
      marginBottom: "12px",
      margin: 0
    },
    trendContainer: {
      display: "flex",
      alignItems: "center",
      gap: "8px",
      marginTop: "12px"
    },
    trendValue: {
      fontSize: "14px",
      fontWeight: "600"
    },
    trendText: {
      color: "#6b7280",
      fontSize: "14px"
    }
  };

  const trendColor = isPositive ? COLORS.textGreen : COLORS.textRed;

  // Ensure trendValue shown safely
  const trendDisplay = trendValue == null ? "-" : Number(trendValue).toLocaleString();

  return (
    <div
      style={{
        ...styles.card,
        ...(isHovered ? styles.cardHover : {})
      }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      role="group"
      aria-label={`${title} KPI card`}
    >
      <div style={styles.header}>
        <div
          style={{
            ...styles.iconContainer,
            backgroundColor: Theme.iconBg,
            color: Theme.iconColor
          }}
        >
          <RenderIcon candidate={IconCandidate} size={24} alt={`${title} icon`} />
        </div>

        <div style={styles.valueContainer}>
          <div style={styles.value}>{formatValue(value, format)}</div>
        </div>
      </div>

      <div>
        <h3 style={styles.title}>{title}</h3>
      </div>

      {trendValue != null && (
        <div style={styles.trendContainer}>
          <RenderIcon candidate={TrendCandidate} size={16} alt={isPositive ? "up" : "down"} />
          <span style={{ ...styles.trendValue, color: trendColor }}>{trendDisplay}%</span>
          <span style={styles.trendText}>Since last month</span>
        </div>
      )}
    </div>
  );
};

export default KPICard;