package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.Analytics.*;
import com.sqli.ecomAnalytics.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final CustomersAnalyticsService customersAnalyticsService;
    private final KpiService kpiService;
    private final ProductsAnalyticsService productsAnalyticsService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final RecommendationAnalyticsService recommendationAnalyticsService;

    public AnalyticsController(
            CustomersAnalyticsService customersAnalyticsService,
            KpiService kpiService,
            ProductsAnalyticsService productsAnalyticsService,
            SalesAnalyticsService salesAnalyticsService, RecommendationAnalyticsService recommendationAnalyticsService
    ) {
        this.customersAnalyticsService = customersAnalyticsService;
        this.kpiService = kpiService;
        this.productsAnalyticsService = productsAnalyticsService;
        this.salesAnalyticsService = salesAnalyticsService;
        this.recommendationAnalyticsService = recommendationAnalyticsService;
    }

    @Operation(summary = "Get customers related analytics",
            responses = {@ApiResponse(responseCode = "200", description = "List of customers returned successfully")})
    @GetMapping("/customerAnalytics")
    public ResponseEntity<CustomerAnalyticsDto> getAnalytics(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam LocalDateTime threshold
    ) {
        return ResponseEntity.ok(customersAnalyticsService.getAnalytics(start, end, threshold));
    }

    @Operation(summary = "Get KPI",
            responses = {@ApiResponse(responseCode = "200", description = "KPI dto is returned")})
    @GetMapping("/kpi")
    public ResponseEntity<KpiDto> getKpi(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {
        return ResponseEntity.ok(kpiService.getKpi(start, end));
    }

    @Operation(summary = "Get products performance",
            responses = {@ApiResponse(responseCode = "200", description = "Products performance dto is returned")})
    @GetMapping("/productsPerformance")
    public ResponseEntity<ProductPerformanceDto> getProductPerformance(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam int threshold
    ) {
        return ResponseEntity.ok(productsAnalyticsService.getProductPerformance(start, end, threshold));
    }

    @Operation(summary = "Get sales analytics",
            responses = {@ApiResponse(responseCode = "200", description = "Sales analytics dto is returned")})
    @GetMapping("/salesAnalytics")
    public ResponseEntity<SalesTrendDto> getSalesTrend(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {
        return ResponseEntity.ok(salesAnalyticsService.getsalesTrend(start, end));
    }

    @Operation(summary = "Get Recommendations analytics",
            responses = {@ApiResponse(responseCode = "200", description = "Recommendation analytics dto is returned")})
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationAnalyticsDto> getRecommendations() {
        RecommendationAnalyticsDto analytics = recommendationAnalyticsService.getRecommendationAnalytics();
        return ResponseEntity.ok(analytics);
    }
}
