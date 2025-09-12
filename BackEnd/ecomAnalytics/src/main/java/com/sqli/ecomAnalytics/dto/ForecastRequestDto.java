package com.sqli.ecomAnalytics.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForecastRequestDto {
    private String requestId;
    private int forecastDays = 30;
    private boolean includeConfidenceIntervals = true;
    private boolean modelComparison = true;
}
