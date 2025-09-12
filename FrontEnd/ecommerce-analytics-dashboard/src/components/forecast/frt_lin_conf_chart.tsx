import React, { useState, useEffect } from 'react';
import {
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Area,
  ComposedChart,
} from 'recharts';
import apiClient from '../../services/api'; // adjust path if necessary

/*
  Self-contained KPICard + Forecast component in one file so you can paste it into a single React file.
  - The Forecast days is editable by the user (number input)
  - Model comparison appears on the right as KPICards
  - Best model is highlighted
  NOTE: If you already have KPICard/ICON_MAP/THEMES elsewhere, replace the KPICard implementation here with your import.
*/

// --- Small icon map and theme fallbacks so KPICard works standalone ---
const ICON_MAP = {
  barChart: ({ width = 24, height = 24 }) => (
    <svg width={width} height={height} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="3" y="10" width="3" height="11" rx="1" fill="currentColor" />
      <rect x="9" y="6" width="3" height="15" rx="1" fill="currentColor" />
      <rect x="15" y="3" width="3" height="18" rx="1" fill="currentColor" />
    </svg>
  ),
  up: ({ width = 16, height = 16 }) => (
    <svg width={width} height={height} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 4v16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M6 10l6-6 6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  down: ({ width = 16, height = 16 }) => (
    <svg width={width} height={width} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 20V4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M18 14l-6 6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
};

const THEMES = {
  blue: { 
    iconBg: '#EFF6FF', 
    iconColor: '#1D4ED8',
    cardBorder: '#3B82F6'
  },
  gray: { 
    iconBg: '#F9FAFB', 
    iconColor: '#4B5563',
    cardBorder: '#E5E7EB'
  },
  green: { 
    iconBg: '#ECFDF5', 
    iconColor: '#059669',
    cardBorder: '#10B981'
  },
};

const COLORS = {
  textGreen: '#059669',
  textRed: '#DC2626',
  background: '#F8FAFC',
};

const KPICard = ({ title, value, trend, trendValue, icon = 'barChart', theme = 'gray', format }) => {
  const [isHovered, setIsHovered] = useState(false);
  const IconCandidate = ICON_MAP[icon];
  const Theme = THEMES[theme] || THEMES.blue;
  const isPositive = trend === 'UP';
  const TrendCandidate = isPositive ? ICON_MAP.up : ICON_MAP.down;

  const RenderIcon = ({ candidate, size = 24, alt = '' }) => {
    if (!candidate) return null;
    const Cmp = candidate;
    return <Cmp width={size} height={size} />;
  };

  const formatValue = (val, fmt) => {
    if (val === null || val === undefined) return '-';
    if (fmt === 'currency') {
      const nf = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
      return nf.format(Number(val));
    }
    if (fmt === 'percentage') {
      return `${Number(val).toLocaleString(undefined, { maximumFractionDigits: 2 })}%`;
    }
    // default numeric formatting
    if (typeof val === 'number') return Number(val).toLocaleString();
    return String(val);
  };

  const isBestModel = theme === 'blue' || theme === 'green';

  const styles = {
    card: {
      backgroundColor: 'white',
      borderRadius: '12px',
      padding: '20px',
      boxShadow: isHovered ? '0 10px 25px rgba(0,0,0,0.15)' : '0 2px 8px rgba(0,0,0,0.08)',
      border: `2px solid ${isBestModel ? Theme.cardBorder : '#E5E7EB'}`,
      transition: 'all 0.3s ease',
      cursor: 'default',
      position: 'relative',
      overflow: 'hidden',
    },
    header: { 
      display: 'flex', 
      alignItems: 'flex-start', 
      justifyContent: 'space-between', 
      marginBottom: '16px' 
    },
    iconContainer: { 
      padding: '12px', 
      borderRadius: '12px', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      backgroundColor: Theme.iconBg,
      color: Theme.iconColor,
      boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
    },
    valueContainer: { 
      textAlign: 'right',
      flex: 1,
      marginLeft: '16px'
    },
    value: { 
      fontSize: '24px', 
      fontWeight: 800, 
      color: '#111827', 
      margin: 0,
      lineHeight: 1.2
    },
    title: { 
      color: '#6B7280', 
      fontSize: '14px', 
      fontWeight: 600, 
      margin: '0 0 12px 0',
      textTransform: 'uppercase',
      letterSpacing: '0.5px'
    },
    trendContainer: { 
      display: 'flex', 
      alignItems: 'center', 
      gap: '6px', 
      marginTop: '16px',
      padding: '8px 12px',
      backgroundColor: '#F8FAFC',
      borderRadius: '8px'
    },
    trendValue: { 
      fontSize: '14px', 
      fontWeight: 700 
    },
    trendText: { 
      color: '#6B7280', 
      fontSize: '13px',
      fontWeight: 500
    },
    bestBadge: {
      position: 'absolute',
      top: '12px',
      right: '12px',
      backgroundColor: Theme.iconColor,
      color: 'white',
      fontSize: '10px',
      fontWeight: 700,
      padding: '4px 8px',
      borderRadius: '12px',
      textTransform: 'uppercase',
      letterSpacing: '0.5px'
    }
  };

  const trendColor = trend === 'UP' ? COLORS.textGreen : COLORS.textRed;
  const trendDisplay = trendValue == null ? '-' : Number(trendValue).toLocaleString(undefined, { maximumFractionDigits: 2 });

  return (
    <div
      style={styles.card}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      role="group"
      aria-label={`${title} KPI card`}
    >
      {isBestModel && <div style={styles.bestBadge}>Best</div>}
      
      <div style={styles.title}>{title}</div>
      
      <div style={styles.header}>
        <div style={styles.iconContainer}>
          <RenderIcon candidate={IconCandidate} size={24} alt={`${title} icon`} />
        </div>
        <div style={styles.valueContainer}>
          <div style={styles.value}>{formatValue(value, format)}</div>
        </div>
      </div>

      {trendValue != null && (
        <div style={styles.trendContainer}>
          <RenderIcon candidate={TrendCandidate} size={16} alt={isPositive ? 'up' : 'down'} />
          <span style={{ ...styles.trendValue, color: trendColor }}>{trendDisplay}%</span>
          <span style={styles.trendText}>vs train</span>
        </div>
      )}
    </div>
  );
};

// --- Main Forecast component ---
const ForecastChart = ({ initialDays = 30 }) => {
  const [forecastDays, setForecastDays] = useState(initialDays);
  const [forecastData, setForecastData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const styles = {
    container: { 
      width: '100%', 
      maxWidth: '1400px', 
      margin: '0 auto', 
      padding: '32px 24px', 
      fontFamily: 'Roboto',
      backgroundColor: COLORS.background,
      minHeight: '100vh'
    },
    headerRow: { 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'space-between', 
      gap: '24px', 
      marginBottom: '32px',
      flexWrap: 'wrap'
    },
    title: { 
      fontSize: '32px', 
      fontWeight: 800, 
      margin: 0,
      color: '#111827',
    },
    controls: { 
      display: 'flex', 
      gap: '16px', 
      alignItems: 'center',
      backgroundColor: 'white',
      padding: '16px 20px',
      borderRadius: '12px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
      border: '1px solid #E5E7EB'
    },
    inputGroup: {
      display: 'flex',
      flexDirection: 'column',
      gap: '4px'
    },
    label: {
      fontSize: '12px',
      fontWeight: 600,
      color: '#6B7280',
      textTransform: 'uppercase',
      letterSpacing: '0.5px'
    },
    input: { 
      padding: '10px 14px', 
      borderRadius: '8px', 
      border: '2px solid #E5E7EB', 
      width: '120px',
      fontSize: '14px',
      fontWeight: 600,
      transition: 'border-color 0.2s ease',
      outline: 'none'
    },
    inputFocus: {
      borderColor: '#3B82F6'
    },
    generateBtn: { 
      padding: '12px 24px', 
      borderRadius: '10px', 
      backgroundColor: '#2563EB', 
      color: 'white', 
      border: 'none', 
      cursor: 'pointer',
      fontSize: '14px',
      fontWeight: 600,
      transition: 'all 0.2s ease',
      boxShadow: '0 4px 12px rgba(37, 99, 235, 0.4)'
    },
    generateBtnHover: {
      backgroundColor: '#1D4ED8',
      transform: 'translateY(-1px)',
      boxShadow: '0 6px 20px rgba(37, 99, 235, 0.4)'
    },
    generateBtnDisabled: {
      backgroundColor: '#9CA3AF',
      cursor: 'not-allowed',
      transform: 'none',
      boxShadow: 'none'
    },
    layout: { 
      display: 'flex', 
      gap: '32px', 
      alignItems: 'flex-start',
      flexWrap: 'wrap'
    },
    chartCard: { 
      flex: '2 1 600px',
      backgroundColor: 'white', 
      border: '1px solid #E5E7EB', 
      borderRadius: '16px', 
      padding: '28px',
      boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
      minWidth: '600px'
    },
    chartTitle: {
      fontSize: '20px',
      fontWeight: 700,
      color: '#111827',
      marginTop: 0,
      marginBottom: '24px'
    },
    sideCard: { 
      flex: '1 1 350px',
      display: 'grid', 
      gap: '16px', 
      maxHeight: '600px', 
      overflowY: 'auto',
      overflowX: 'hidden',
      minWidth: '320px'
    },
    sideHeader: {
      backgroundColor: 'white',
      padding: '20px',
      borderRadius: '12px',
      border: '1px solid #E5E7EB',
      boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
      position: 'sticky',
      top: 0,
      zIndex: 10
    },
    sideHeaderContent: {
      display: 'flex', 
      justifyContent: 'space-between', 
      alignItems: 'center'
    },
    sideTitle: {
      fontSize: '16px',
      fontWeight: 700,
      color: '#111827',
      margin: 0
    },
    bestModelBadge: {
      fontSize: '12px',
      fontWeight: 600,
      color: '#059669',
      backgroundColor: '#ECFDF5',
      padding: '4px 12px',
      borderRadius: '20px',
      border: '1px solid #A7F3D0'
    },
    spinner: { 
      width: '48px', 
      height: '48px', 
      border: '4px solid #F3F4F6', 
      borderTop: '4px solid #3B82F6', 
      borderRadius: '50%', 
      animation: 'spin 1s linear infinite', 
      margin: '48px auto' 
    },
    loadingContainer: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: '16px',
      padding: '48px'
    },
    loadingText: {
      color: '#6B7280',
      fontSize: '16px',
      fontWeight: 500
    },
    errorCard: {
      backgroundColor: '#FEF2F2',
      border: '2px solid #FECACA',
      padding: '16px 20px',
      borderRadius: '12px',
      color: '#B91C1C',
      marginBottom: '24px',
      fontSize: '14px',
      fontWeight: 500
    },
    summaryContainer: {
      marginTop: '24px',
      display: 'flex',
      gap: '16px',
      alignItems: 'center',
      flexWrap: 'wrap'
    },
    summaryBadge: {
      padding: '12px 16px',
      borderRadius: '10px',
      border: '1px solid #E5E7EB',
      backgroundColor: '#F8FAFC',
      fontSize: '13px',
      fontWeight: 500,
      color: '#374151'
    },
    noDataText: {
      color: '#9CA3AF',
      fontSize: '14px',
      fontStyle: 'italic',
      textAlign: 'center',
      padding: '24px'
    }
  };

  useEffect(() => {
    // initial load
    fetchForecast(initialDays);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchForecast = async (days = forecastDays) => {
    setLoading(true);
    setError(null);
    try {
      const payload = { forecastDays: Number(days), includeConfidenceIntervals: true, modelComparison: true };
      const res = await apiClient.post('/forecasting/generate', payload);
      // accommodate both axios-like and direct responses
      const data = res && res.data ? res.data : res;

      if (!data || !Array.isArray(data.forecastData)) {
        throw new Error('Unexpected response shape from forecast API');
      }
      setForecastData(data);
    } catch (err) {
      const message = err && err.message ? err.message : String(err);
      setError(message);
      console.error('Forecast fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  const formatCurrency = (value) => {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
  };

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{ 
          backgroundColor: 'white', 
          padding: '16px', 
          border: '1px solid #E5E7EB', 
          borderRadius: '12px',
          boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
          minWidth: '180px'
        }}>
          <div style={{ fontWeight: 700, marginBottom: '8px', color: '#111827' }}>{`Date: ${label}`}</div>
          {payload.map((entry, i) => (
            <div key={i} style={{ fontSize: 14, color: entry.color, marginBottom: '4px', fontWeight: 500 }}>
              {`${entry.name}: ${formatCurrency(entry.value)}`}
            </div>
          ))}
        </div>
      );
    }
    return null;
  };

  const chartData = (forecastData && forecastData.forecastData) ? forecastData.forecastData.map(item => ({ 
    date: formatDate(item.date), 
    actualSales: item.actualSales, 
    predictedSales: item.predictedSales, 
    confidenceUpper: item.confidenceUpper, 
    confidenceLower: item.confidenceLower 
  })) : [];

  return (
    <div style={styles.container}>
      <div style={styles.headerRow}>
        <h1 style={styles.title}>Sales Forecast</h1>

        <div style={styles.controls}>
          <div style={styles.inputGroup}>
            <label htmlFor="days" style={styles.label}>Forecast Days</label>
            <input 
              id="days" 
              type="number" 
              min={1} 
              max={365} 
              value={forecastDays} 
              onChange={(e) => setForecastDays(Number(e.target.value))} 
              style={{
                ...styles.input,
                ...(document.activeElement?.id === 'days' ? styles.inputFocus : {})
              }}
            />
          </div>
          <button 
            style={{
              ...styles.generateBtn,
              ...(loading ? styles.generateBtnDisabled : {})
            }}
            onClick={() => fetchForecast(forecastDays)} 
            disabled={loading}
            onMouseEnter={(e) => {
              if (!loading) {
                Object.assign(e.target.style, styles.generateBtnHover);
              }
            }}
            onMouseLeave={(e) => {
              if (!loading) {
                Object.assign(e.target.style, styles.generateBtn);
              }
            }}
          >
            {loading ? 'Generating...' : 'Generate Forecast'}
          </button>
        </div>
      </div>

      {error && (
        <div style={styles.errorCard}>
          <strong>Error:</strong> {error}
        </div>
      )}

      <div style={styles.layout}>
        <div style={styles.chartCard}>
          {loading && (
            <div style={styles.loadingContainer}>
              <div style={styles.spinner} />
              <div style={styles.loadingText}>Generating forecast...</div>
              <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
            </div>
          )}

          {!loading && (
            <>
              <h3 style={styles.chartTitle}>Sales Forecast with Confidence Intervals</h3>

              <ResponsiveContainer width="100%" height={480}>
                <ComposedChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
                  <defs>
                    <linearGradient id="confidenceGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.15} />
                      <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>

                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" opacity={0.6} />
                  <XAxis 
                    dataKey="date" 
                    tick={{ fontSize: 12, fill: '#6B7280' }} 
                    angle={-45} 
                    textAnchor="end" 
                    height={80}
                    stroke="#9CA3AF"
                  />
                  <YAxis 
                    tick={{ fontSize: 12, fill: '#6B7280' }} 
                    tickFormatter={(v) => `$${(v/1000).toFixed(0)}k`}
                    stroke="#9CA3AF"
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend 
                    wrapperStyle={{ paddingTop: '20px' }}
                    iconType="line"
                  />

                  <Area 
                    type="monotone" 
                    dataKey="confidenceUpper" 
                    stroke="none" 
                    fill="url(#confidenceGradient)" 
                    name="Confidence Upper" 
                  />
                  <Area 
                    type="monotone" 
                    dataKey="confidenceLower" 
                    stroke="none" 
                    fill="#ffffff" 
                    name="Confidence Lower" 
                  />

                  <Line 
                    type="monotone" 
                    dataKey="actualSales" 
                    stroke="#059669" 
                    strokeWidth={3} 
                    dot={{ r: 5, fill: "#059669", strokeWidth: 2, stroke: "#ffffff" }} 
                    name="Actual Sales"
                    activeDot={{ r: 7, fill: "#059669" }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="predictedSales" 
                    stroke="#3B82F6" 
                    strokeWidth={3} 
                    strokeDasharray="8 4" 
                    dot={{ r: 4, fill: "#3B82F6", strokeWidth: 2, stroke: "#ffffff" }} 
                    name="Predicted Sales"
                    activeDot={{ r: 6, fill: "#3B82F6" }}
                  />

                  <Line 
                    type="monotone" 
                    dataKey="confidenceUpper" 
                    stroke="#94A3B8" 
                    strokeWidth={1.5} 
                    dot={false} 
                    name="Upper Confidence"
                    strokeDasharray="2 2"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="confidenceLower" 
                    stroke="#94A3B8" 
                    strokeWidth={1.5} 
                    dot={false} 
                    name="Lower Confidence"
                    strokeDasharray="2 2"
                  />
                </ComposedChart>
              </ResponsiveContainer>

              {forecastData && forecastData.summary && (
                <div style={styles.summaryContainer}>
                  <div style={styles.summaryBadge}>
                    <strong>Total Points:</strong> {forecastData.summary.totalDataPoints}
                  </div>
                  <div style={styles.summaryBadge}>
                    <strong>Horizon:</strong> {forecastData.summary.forecastHorizonDays} days
                  </div>
                  <div style={styles.summaryBadge}>
                    <strong>Avg CI Width:</strong> {formatCurrency(forecastData.summary.averageConfidenceIntervalWidth)}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Right side - model KPIs */}
        <div style={styles.sideCard}>
          <div style={styles.sideHeader}>
            <div style={styles.sideHeaderContent}>
              <h4 style={styles.sideTitle}>Model Performance</h4>
              {forecastData && forecastData.bestModel && (
                <div style={styles.bestModelBadge}>
                  Best: {forecastData.bestModel}
                </div>
              )}
            </div>
          </div>

          {!forecastData && (
            <div style={styles.noDataText}>
              No model data available. Generate a forecast to see performance metrics.
            </div>
          )}

          {forecastData && Array.isArray(forecastData.modelMetrics) && forecastData.modelMetrics.map((m) => {
            const isBest = forecastData.bestModel && m.modelName === forecastData.bestModel;
            const maeImprovementPct = m.trainMae ? ((m.trainMae - m.testMae) / m.trainMae) * 100 : null;
            const rmseImprovementPct = m.trainRmse ? ((m.trainRmse - m.testRmse) / m.trainRmse) * 100 : null;

            return (
              <div key={m.modelName} style={{ display: 'grid', gap: '12px' }}>
                <KPICard 
                  title={`${m.modelName} — Test MAE`} 
                  value={m.testMae} 
                  format="currency" 
                  trend={maeImprovementPct != null && maeImprovementPct > 0 ? 'UP' : 'DOWN'} 
                  trendValue={maeImprovementPct != null ? Math.abs(maeImprovementPct) : null} 
                  icon="barChart" 
                  theme={isBest ? 'blue' : 'gray'} 
                />

                <KPICard 
                  title={`${m.modelName} — Test RMSE`} 
                  value={m.testRmse} 
                  format="currency" 
                  trend={rmseImprovementPct != null && rmseImprovementPct > 0 ? 'UP' : 'DOWN'} 
                  trendValue={rmseImprovementPct != null ? Math.abs(rmseImprovementPct) : null} 
                  icon="barChart" 
                  theme={isBest ? 'green' : 'gray'} 
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ForecastChart;