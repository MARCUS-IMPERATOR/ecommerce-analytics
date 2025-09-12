import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import apiClient from '../../services/api';

interface ProductTurnover {
  productId: number;
  productName: string;
  currentStock: number;
  totalSold: number;
  turnoverRate: number;
}

interface TurnoverRateChartProps {
    startDate : string;
    endDate : string;
    lowStockThreshold : number;

}

const TurnoverRateChart : React.FC<TurnoverRateChartProps> = ({
    startDate,
    endDate,
    lowStockThreshold,
}) => {
  const [chartData, setChartData] = useState<ProductTurnover[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchTurnoverData = async () => {
      try {
        const response = await apiClient.get<{ inventoryAnalysis: ProductTurnover[] }>(
          `analytics/productsPerformance?start=${startDate}T00:00:00&end=${endDate}T23:59:59&threshold=${lowStockThreshold}`
        );
        const turnoverData = response.inventoryAnalysis.map((item) => ({
          name: item.productName,
          rate: (item.turnoverRate * 100).toFixed(2),
          sold: item.totalSold,
          stock: item.currentStock
        }));
        setChartData(turnoverData);
      } catch (error) {
        setError('Failed to load turnover data');
      } finally {
        setLoading(false);
      }
    };

    fetchTurnoverData();
  }, []);

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div style={{
          backgroundColor: 'white',
          padding: '10px',
          border: '1px solid #ccc',
          borderRadius: '5px',
          boxShadow: '0 2px 5px rgba(0,0,0,0.1)'
        }}>
          <p style={{ margin: 0, fontWeight: 'bold' }}>{data.name}</p>
          <p style={{ margin: '5px 0', color: '#8884d8' }}>
            Turnover Rate: {data.rate}%
          </p>
          <p style={{ margin: 0, fontSize: '12px', color: '#666' }}>
            Sold: {data.sold} | Stock: {data.stock}
          </p>
        </div>
      );
    }
    return null;
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div style={{ width: '100%', height: '600px', padding: '20px' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '20px', color: '#333' }}>
        Top Products by Inventory Turnover Rate
      </h2>
      
      <div style={{
        backgroundColor: '#f5f5f5',
        padding: '10px',
        borderRadius: '5px',
        marginBottom: '20px',
        textAlign: 'center'
      }}>
        <small style={{ color: '#666' }}>
          Turnover Rate = (Units Sold ÷ Current Stock) × 100%
        </small>
      </div>

      <ResponsiveContainer width="100%" height={450}>
        <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="name"
            angle={-45}
            textAnchor="end"
            height={80}
            fontSize={11}
          />
          <YAxis
            label={{ value: 'Turnover Rate (%)', angle: -90, position: 'insideLeft' }}
            fontSize={11}
          />
          <Tooltip content={<CustomTooltip />} />
          <Bar
            dataKey="rate"
            fill="#8884d8"
            radius={[3, 3, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TurnoverRateChart;