import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import apiClient from '../../services/api';

interface CategoryPerformanceData {
  category: string;
  totalQuantitySold: number;
  totalRevenue: number;
  productCount: number;
  averagePrice: number;
}

interface ProductPerformanceResponse {
  categoryPerformance: CategoryPerformanceData[];
}

interface CategoryPerformanceChartProps {
    startDate: string;
    endDate: string;
    lowStockThreshold: number;
}

const CategoryPerformanceChart: React.FC<CategoryPerformanceChartProps> = ({
    startDate,
    endDate,
    lowStockThreshold
}) => {
  const [data, setData] = useState<CategoryPerformanceData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Using the apiClient to make the GET request
      const response = await apiClient.get<ProductPerformanceResponse>(`analytics/productsPerformance`, {
        params: {
          start: `${startDate}T00:00:00`,
          end: `${endDate}T23:59:59`,
          threshold: lowStockThreshold
        }
      });

      setData(response.categoryPerformance);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [startDate, endDate, lowStockThreshold]);  // Added dependencies to re-fetch data when these change

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  };

  const formatTooltip = (value: any, name: string) => {
    if (name === 'totalRevenue') {
      return [formatCurrency(value), 'Revenue'];
    }
    return [value, name === 'totalQuantitySold' ? 'Quantity Sold' : name];
  };

  const chartData = data.map(item => ({
    category: item.category,
    totalQuantitySold: item.totalQuantitySold,
    totalRevenue: item.totalRevenue / 100,  // Assuming revenue is in cents, divide by 100
    productCount: item.productCount,
    averagePrice: item.averagePrice
  }));

  const styles = {
    container: {
      width: '100%',
      maxWidth: '1200px',
      margin: '0 auto',
      padding: '24px',
      backgroundColor: '#ffffff',
      borderRadius: '8px',
      boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
      fontFamily: 'Arial, sans-serif'
    },
    header: {
      marginBottom: '24px'
    },
    title: {
      fontSize: '24px',
      fontWeight: 'bold',
      color: '#1f2937',
      marginBottom: '16px',
      margin: '0 0 16px 0'
    },
    errorAlert: {
      marginBottom: '16px',
      padding: '12px',
      backgroundColor: '#fef3c7',
      border: '1px solid #f59e0b',
      color: '#92400e',
      borderRadius: '6px'
    },
    loadingContainer: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '384px'
    },
    loadingText: {
      fontSize: '18px',
      color: '#6b7280'
    },
    chartContainer: {
      height: '384px',
      marginBottom: '24px'
    },
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer}>
        <div style={styles.loadingText}>Loading chart data...</div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Product Performance by Category</h2>
        {error && <div style={styles.errorAlert}>{error}</div>}
      </div>

      <div style={styles.chartContainer}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={chartData}
            margin={{
              top: 20,
              right: 30,
              left: 20,
              bottom: 5,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis 
              dataKey="category" 
              angle={-45}
              textAnchor="end"
              height={80}
              fontSize={12}
            />
            <YAxis 
              yAxisId="left"
              label={{ value: 'Quantity Sold', angle: -90, position: 'insideLeft' }}
            />
            <YAxis 
              yAxisId="right" 
              orientation="right"
              label={{ value: 'Revenue (×100)', angle: 90, position: 'insideRight' }}
            />
            <Tooltip 
              formatter={formatTooltip}
              labelStyle={{ color: '#374151' }}
              contentStyle={{ 
                backgroundColor: '#f9fafb', 
                border: '1px solid #e5e7eb',
                borderRadius: '6px'
              }}
            />
            <Legend />
            <Bar 
              yAxisId="left"
              dataKey="totalQuantitySold" 
              fill="#3b82f6" 
              name="Quantity Sold"
              radius={[2, 2, 0, 0]}
            />
            <Bar 
              yAxisId="right"
              dataKey="totalRevenue" 
              fill="#ef4444" 
              name="Revenue (×100)"
              radius={[2, 2, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default CategoryPerformanceChart;