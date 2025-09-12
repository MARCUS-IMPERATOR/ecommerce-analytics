import React, { useState, useEffect, useMemo } from 'react';
import apiClient from '../../services/api';

interface DailySalesData {
  date: string;
  revenue: number;
  orderCount: number;
}

interface MonthlySalesData {
  year: number;
  month: number;
  revenue: number;
  orderCount: number;
}

interface SalesData {
  dailySales: DailySalesData[];
  monthlySales: MonthlySalesData[];
  trendDirection: string;
}

interface Props {
    startDate : string;
    endDate : string;
}

const SalesHeatmap: React.FC<Props> = ({
    startDate,
    endDate
}) => {
  const [salesData, setSalesData] = useState<SalesData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSalesData = async () => {
      try {
        const response = await apiClient.get('analytics/salesAnalytics', {
          params: {
            start: `${startDate}T00:00:00`,
            end: `${endDate}T23:59:59`,
          },
        });
        setSalesData(response);
        setLoading(false);
      } catch (err) {
        setError('Failed to fetch data');
        setLoading(false);
      }
    };

    fetchSalesData();
  }, [startDate,endDate]);

  const months = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
  ];

  const heatmapData = useMemo(() => {
    if (!salesData) return { data: [], years: [] }; // Handle null data gracefully

    const monthlyDataMap = new Map<string, MonthlySalesData>();
    salesData.monthlySales.forEach(data => {
      const key = `${data.year}-${data.month}`;
      monthlyDataMap.set(key, data);
    });

    const years = [...new Set(salesData.monthlySales.map(d => d.year))].sort();
    const data: Array<{ year: number; month: number; trend: 'UP' | 'DOWN' | 'STABLE'; revenue: number; orderCount: number }> = [];

    years.forEach(year => {
      for (let month = 1; month <= 12; month++) {
        const currentKey = `${year}-${month}`;
        const prevMonth = month === 1 ? 12 : month - 1;
        const prevYear = month === 1 ? year - 1 : year;
        const prevKey = `${prevYear}-${prevMonth}`;

        const currentData = monthlyDataMap.get(currentKey);
        const prevData = monthlyDataMap.get(prevKey);

        if (currentData) {
          let trend: 'UP' | 'DOWN' | 'STABLE' = 'STABLE';
          
          if (prevData) {
            if (currentData.revenue > prevData.revenue) {
              trend = 'UP';
            } else if (currentData.revenue < prevData.revenue) {
              trend = 'DOWN';
            }
          }

          data.push({
            year,
            month,
            trend,
            revenue: currentData.revenue,
            orderCount: currentData.orderCount
          });
        }
      }
    });

    return { data, years };
  }, [salesData]);

  const getTrendColor = (trend: 'UP' | 'DOWN' | 'STABLE') => {
    switch (trend) {
      case 'UP':
        return '#22c55e'; // Green
      case 'DOWN':
        return '#ef4444'; // Red
      case 'STABLE':
      default:
        return '#9ca3af'; // Gray
    }
  };

  const getCellData = (year: number, month: number) => {
    return heatmapData.data.find(d => d.year === year && d.month === month);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2 style={{ marginBottom: '20px', color: '#1f2937', fontSize: '24px', fontWeight: 'bold' }}>
        Sales Trend Heatmap
      </h2>
      
      <div style={{ marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px', fontSize: '14px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{ width: '16px', height: '16px', backgroundColor: '#22c55e' }}></div>
            <span>Trending Up</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{ width: '16px', height: '16px', backgroundColor: '#ef4444' }}></div>
            <span>Trending Down</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <div style={{ width: '16px', height: '16px', backgroundColor: '#9ca3af' }}></div>
            <span>Stable</span>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {/* Header row with months */}
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <div style={{ width: '60px', fontSize: '14px', fontWeight: 'bold', color: '#6b7280' }}></div>
          {months.map((month) => (
            <div key={month} style={{ 
              width: '50px', 
              textAlign: 'center',
              fontSize: '14px',
              fontWeight: '500',
              color: '#6b7280'
            }}>
              {month}
            </div>
          ))}
        </div>

        {/* Data rows */}
        {heatmapData.years.map(year => (
          <div key={year} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <div style={{ 
              width: '60px',
              fontSize: '14px',
              fontWeight: 'bold',
              color: '#6b7280',
              textAlign: 'right'
            }}>
              {year}
            </div>
            {months.map((_, monthIndex) => {
              const month = monthIndex + 1;
              const cellData = getCellData(year, month);
              
              return (
                <div
                  key={`${year}-${month}`}
                  style={{
                    width: '50px',
                    height: '50px',
                    backgroundColor: cellData ? getTrendColor(cellData.trend) : '#e5e7eb',
                    borderRadius: '12px',
                    cursor: cellData ? 'pointer' : 'default',
                    transition: 'all 0.2s ease',
                    position: 'relative'
                  }}
                  title={cellData ? 
                    `${months[monthIndex]} ${year}\nRevenue: ${cellData.revenue.toFixed(2)}\nOrders: ${cellData.orderCount}\nTrend: ${cellData.trend}` : 
                    'No data'
                  }
                  onMouseEnter={(e) => {
                    if (cellData) {
                      e.currentTarget.style.transform = 'scale(1.15)';
                      e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.2)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1)';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                />
              );
            })}
          </div>
        ))}
      </div>

      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
        <h3 style={{ margin: '0 0 10px 0', fontSize: '16px', fontWeight: 'bold', color: '#1f2937' }}>
          Summary Statistics
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
          <div>
            <div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>Overall Trend</div>
           <div style={{fontSize: '16px',fontWeight: 'bold',color: salesData?.trendDirection === 'UP' ? '#27AE60' : salesData?.trendDirection === '#EB5757' ? 'red' : '#1f2937',}}>
                {salesData?.trendDirection || 'No data'}
            </div>
          </div>
          {/* Add other summary statistics here */}
        </div>
      </div>
    </div>
  );
};

export default SalesHeatmap;