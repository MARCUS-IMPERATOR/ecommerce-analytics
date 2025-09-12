import React, { useEffect, useState } from "react";
import RecommendationKPIs from "../components/recommendation_analytics/rec_kpi";
import TopRecommendedProductsTable from "../components/recommendation_analytics/rec_tab";

const RecommendationAnalytics: React.FC = () => {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate data loading
    const timer = setTimeout(() => setLoading(false), 2000);
    return () => clearTimeout(timer);
  }, []);

  const styles = {
    container: {
      fontFamily: "Roboto, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      padding: "2rem",
      backgroundColor: "#f8fafc",
      minHeight: "100vh",
    } as React.CSSProperties,

    header: {
      marginBottom: "2rem",
    } as React.CSSProperties,

    title: {
      fontSize: "2rem",
      fontWeight: 700,
      color: "#1e293b",
      margin: 0,
      letterSpacing: "-0.025em",
    } as React.CSSProperties,

    chartsContainer: {
      display: "grid",
      gridTemplateColumns: "1fr",
      gap: "1.25rem",
      "@media (min-width: 1024px)": {
        gridTemplateColumns: "1fr 1fr",
      },
    } as React.CSSProperties,

    chartCard: {
      backgroundColor: "white",
      borderRadius: "0.75rem",
      padding: "1.5rem",
      boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
      border: "1px solid #e2e8f0",
      transition: "box-shadow 0.15s ease-in-out",
    } as React.CSSProperties,

    spinnerContainer: {
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      height: "200px",
      flexDirection: "column",
    } as React.CSSProperties,

    spinner: {
      width: "40px",
      height: "40px",
      border: "4px solid #e2e8f0",
      borderTop: "4px solid #3b82f6",
      borderRadius: "50%",
      animation: "spin 1s linear infinite",
    } as React.CSSProperties,

    loadingText: {
      marginTop: "1rem",
      color: "#64748b",
      fontSize: "0.875rem",
    } as React.CSSProperties,
  };

  // Add keyframes for spinner animation
  useEffect(() => {
    const styleId = "spinner-animation";
    
    // Check if style already exists
    if (!document.getElementById(styleId)) {
      const styleEl = document.createElement("style");
      styleEl.id = styleId;
      styleEl.innerHTML = `
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `;
      document.head.appendChild(styleEl);
    }

    // Cleanup function - only remove if no other instances need it
    return () => {
      // Note: In a real app, you might want to implement reference counting
      // to avoid removing styles that other components might still need
    };
  }, []);

  const LoadingSpinner = () => (
    <div style={styles.spinnerContainer}>
      <div style={styles.spinner}></div>
      <div style={styles.loadingText}>Loading analytics...</div>
    </div>
  );

  const ChartCard: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <div style={styles.chartCard}>
      {loading ? <LoadingSpinner /> : children}
    </div>
  );

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1 style={styles.title}>Recommendation Analytics</h1>
      </header>

      <main style={styles.chartsContainer}>
        <ChartCard>
          <RecommendationKPIs />
        </ChartCard>

        <ChartCard>
          <TopRecommendedProductsTable />
        </ChartCard>
      </main>
    </div>
  );
};

export default RecommendationAnalytics;