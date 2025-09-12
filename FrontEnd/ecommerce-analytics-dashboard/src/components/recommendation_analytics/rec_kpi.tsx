import React, { useEffect, useState } from "react";
import KPICard from "../KpiCard";
import apiClient from "../../services/api";
import { ICON_MAP } from "../icon_map";
const RecommendationKPIs = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchRecommendationAnalytics() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiClient.get("/analytics/recommendations");
        setData(response);
      } catch (err) {
        setError(err.message || "Failed to fetch data");
      } finally {
        setLoading(false);
      }
    }

    fetchRecommendationAnalytics();
  }, []);

  if (loading) return <div>Loading recommendations...</div>;

  if (error) return <div>Error loading recommendations: {error}</div>;

  if (!data) return <div>No recommendation data available</div>;

  const totalCustomers = data.totalCustomers;
  const averageRecommendations = data.averageRecommendations;

  const totalWeightedScore = data.topRecommendedProducts.reduce(
    (acc, item) => acc + item.recommendationCount * item.averageScore,
    0
  );

  const totalRecommendations = data.topRecommendedProducts.reduce(
    (acc, item) => acc + item.recommendationCount,
    0
  );

  const averageRecommendationScore =
    totalRecommendations > 0 ? totalWeightedScore / totalRecommendations : 0;

  return (
    <div style={{ display: "flex", gap: "24px", width: "100%" }}>
      <div style={{ flex: 1 }}>
        <KPICard
          title="Customers with Recommendations"
          value={totalCustomers}
          trend={null}
          trendValue={null}
          icon="stats"
          theme="green"
          format="number"
        />
      </div>
      <div style={{ flex: 1 }}>
        <KPICard
          title="Avg Recommendations per Customer"
          value={averageRecommendations.toFixed(2)}
          trend={null}
          trendValue={null}
          icon="number"
          theme="blue"
          format="number"
        />
      </div>
      <div style={{ flex: 1 }}>
        <KPICard
          title="Avg Recommendation Score"
          value={averageRecommendationScore.toFixed(3)}
          trend={null}
          trendValue={null}
          icon="percent"
          theme="yellow"
          format="number"
        />
      </div>
    </div>
  );
};

export default RecommendationKPIs;