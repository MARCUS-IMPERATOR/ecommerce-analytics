package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForecastResponse {
    private boolean success;
    private String error;
    private String bestModel;
    private List<ModelMetrics> modelMetrics;
    private List<ForecastPoint> forecastData;
    private ForecastSummary summary;
    private long processingTimeMs;
}
