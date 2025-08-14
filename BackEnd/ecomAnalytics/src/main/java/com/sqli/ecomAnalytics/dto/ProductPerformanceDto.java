package com.sqli.ecomAnalytics.dto;

import com.sqli.ecomAnalytics.entity.Products;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductPerformanceDto {
    private List<TopProductData> topSellingProducts;
    private List<CategoryPerformanceData> categoryPerformance;
    private List<InventoryTurnoverData> inventoryAnalysis;
    private List<Products> lowStockAlerts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProductData {
        private int productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryPerformanceData {
        private String category;
        private Long totalQuantitySold;
        private BigDecimal totalRevenue;
        private int productCount;
        private BigDecimal averagePrice;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InventoryTurnoverData {
        private int productId;
        private String productName;
        private int currentStock;
        private Long totalSold;
        private Double turnoverRate;
    }
}
