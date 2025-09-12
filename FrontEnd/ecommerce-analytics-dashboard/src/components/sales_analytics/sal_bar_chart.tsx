import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, Cell } from 'recharts';
import apiClient from '../../services/api';

interface Props {
    startDate : string;
    endDate : string;
}

const MonthlySalesChart : React.FC<Props> = ({
    startDate,
    endDate
}) => {
  const [monthlySales, setMonthlySales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSalesData = async () => {
      try {
        const response = await apiClient.get('analytics/salesAnalytics', {
          params: {
            start: `${startDate}T00:00:00`,
            end: `${endDate}T23:59:59`
          }
        });
        setMonthlySales(response.monthlySales);
        setLoading(false);
      } catch (err) {
        setError('Failed to fetch data');
        setLoading(false);
      }
    };

    fetchSalesData();
  }, [startDate,endDate]);

  const monthColors: Record<number, string> = {
    1: "#050215",  // Jan
    2: "#0A072C",  // Feb
    3: "#16124F",  // Mar
    4: "#1F2075",  // Apr
    5: "#3A2D9E",  // May
    6: "#513CC8",  // Jun
    7: "#514BF4",  // Jul
    8: "#6A5BCF",  // Aug
    9: "#7D63E2",  // Sep
    10: "#A478F7", // Oct
    11: "#B28BFF", // Nov
    12: "#D1A7FF"  // Dec
  };

  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  const transformedData = monthlySales.map(item => ({
    ...item,
    monthYear: `${monthNames[item.month - 1]} ${item.year}`,
    formattedRevenue: item.revenue.toFixed(2)
  }));

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div style={{
          backgroundColor: '#ffffff',
          border: '1px solid #ccc',
          borderRadius: '8px',
          padding: '12px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
        }}>
          <p style={{ margin: '0 0 8px 0', fontWeight: 'bold', color: '#333' }}>
            {label}
          </p>
          <p style={{ margin: '0 0 4px 0', color: '#2563eb' }}>
            Revenue: ${data.revenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </p>
          <p style={{ margin: '0', color: '#16a34a' }}>
            Orders: {data.orderCount}
          </p>
        </div>
      );
    }
    return null;
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div style={{ 
      width: '100%', 
      height: '600px', 
      padding: '20px',
      backgroundColor: '#f8fafc',
      fontFamily: 'Arial, sans-serif'
    }}>
      <div style={{ marginBottom: '20px', textAlign: 'center' }}>
        <h2 style={{ 
          margin: '0 0 8px 0', 
          color: '#1e293b', 
          fontSize: '28px',
          fontWeight: 'bold'
        }}>
          Monthly Sales Analytics
        </h2>
      </div>
      
      <ResponsiveContainer width="100%" height="90%">
        <BarChart
          data={transformedData}
          margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis 
            dataKey="monthYear" 
            angle={-45}
            textAnchor="end"
            height={80}
            tick={{ fontSize: 12, fill: '#475569' }}
            axisLine={{ stroke: '#cbd5e1' }}
          />
          <YAxis 
            tick={{ fontSize: 12, fill: '#475569' }}
            axisLine={{ stroke: '#cbd5e1' }}
            tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend 
            verticalAlign="top"
            height={36}
            iconType="rect"
            wrapperStyle={{ paddingBottom: '20px' }}
          />
          <Bar dataKey="revenue" name="Revenue ($)" radius={[4, 4, 0, 0]} strokeWidth={1}>
            {transformedData.map((entry, index) => (
              <Cell 
                key={`cell-${index}`} 
                fill={monthColors[entry.month]} 
                stroke={monthColors[entry.month]} 
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default MonthlySalesChart;