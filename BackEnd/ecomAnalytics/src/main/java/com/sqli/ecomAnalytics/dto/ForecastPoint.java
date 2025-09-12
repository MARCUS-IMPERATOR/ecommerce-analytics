package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForecastPoint {
    private String date;
    private Double actualSales;
    private double predictedSales;
    private double confidenceLower;
    private double confidenceUpper;
}
