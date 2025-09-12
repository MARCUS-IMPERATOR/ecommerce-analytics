import React from "react"; 
import CustomerAnalytics from "../features/customer_analytics";
import Kpi from "../features/Kpi";
import ProductPerformance from "../features/product_performance";
import SalesAnalytics from "../features/sales_analytics";
import RecommendationAnalytics from "../features/recommendation_analytics";
import ForecastChart from "../components/forecast/frt_lin_conf_chart";
const DashBoard = () => { 
    return(
        <div style={{ 
            display: 'flex',
            flexDirection:'column', 
            backgroundColor: '#F5F5F5', 
            gap: '50px' 
        }}>
            <Kpi />
            <CustomerAnalytics />
            <ProductPerformance/>
            <SalesAnalytics/>
            <RecommendationAnalytics/>
            <ForecastChart/>
        </div>
    )
}

export default DashBoard;