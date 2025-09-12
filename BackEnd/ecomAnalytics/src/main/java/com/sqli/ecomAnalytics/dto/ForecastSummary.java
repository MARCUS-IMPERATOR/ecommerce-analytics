package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForecastSummary {
    private int totalDataPoints;
    private int forecastHorizonDays;
    private double averageConfidenceIntervalWidth;
    private double modelAccuracyScore;
}
