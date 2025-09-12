package com.sqli.ecomAnalytics.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationAnalyticsDto {
    private List<ProductRecommendationDto> topRecommendedProducts;
    private long totalCustomers;
    private double averageRecommendations;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductRecommendationDto {
        private int productId;
        private String productName;
        private Long recommendationCount;
        private double averageScore;
    }
}
