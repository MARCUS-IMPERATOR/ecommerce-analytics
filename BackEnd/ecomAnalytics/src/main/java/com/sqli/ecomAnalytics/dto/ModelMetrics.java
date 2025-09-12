package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelMetrics {
    private String modelName;
    private double trainMae;
    private double testMae;
    private double trainRmse;
    private double testRmse;
}
